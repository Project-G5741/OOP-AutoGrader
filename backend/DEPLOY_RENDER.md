Render deployment steps (Docker)

1. In Render dashboard, create a new Web Service -> "Docker" or "Connect a repo" and select this repository.
2. Set the root directory to `backend` (Render will use the Dockerfile in that folder).
3. Environment variables to add:
   - `SPRING_DATASOURCE_URL` = `jdbc:postgresql://<POOLER-HOST>-pooler.<region>.aws.neon.tech:5432/<DB_NAME>?sslmode=require&socketTimeout=30` — use the **pooler** hostname (`-pooler` in the host) for long-lived JVM deployments on Render. Prefer `socketTimeout=30` on the URL (see `backend/.env.backend.example`); `application.properties` defaults are **sleep-friendly** (`minimum-idle=0`, `idle-timeout=10s`, Hikari keepalive off) so Railway Serverless can scale to zero — on always-on Render you may optionally warm the pool with `HIKARI_MINIMUM_IDLE=2` and `HIKARI_KEEPALIVE_TIME=120000`. `stringtype=unspecified` so batched OT enum inserts (lab clone/sync) cast into Postgres native enums. The direct (non-pooler) endpoint can exhaust Neon connection limits with default Hikari pool size (~10).
   - `DB_USERNAME` = <db user>
   - `DB_PASSWORD` = <db pass>
   - `FRONTEND_URL` = https://oop-autograder.vercel.app
   - `JWT_SECRET` = <your-jwt-secret, at least 32 bytes; generate with `openssl rand -base64 32`. Changing this invalidates all existing sessions.>
   - `GOOGLE_CLIENT_ID` = <google client id>
   - `SPRINGDOC_ENABLED` = `false` (do not register OpenAPI/Swagger in production)
   - `JAVA_OPTS` = same as `backend/Dockerfile` (default `-Xmx192m`, Metaspace `96m`, code cache `32m`, G1 periodic GC). Do **not** set `-Xmx512m`. Railway notes: [DEPLOY_RAILWAY.md](./DEPLOY_RAILWAY.md).
   - `PRACTICE_BUNDLE_URL` = public GitHub Release asset URL (step 8); **runtime** on the API container (prefetched on startup)
   - `LECTURER_BULK_BUNDLE_URL` = public GitHub Release asset URL (step 9); same. Not a frontend variable.

   Desktop practice **version label** (web nav + desktop header) lives in `frontend/public/practice-bundle-version.json` — bump on release, redeploy the web frontend, and assemble/publish the practice zip (not API env).

   **Password-reset email (Render free tier):** Render blocks outbound SMTP (ports 25/465/587). Use the Brevo HTTPS API instead of Gmail SMTP:
   - `MAIL_PROVIDER` = `brevo`
   - `BREVO_API_KEY` = <Brevo API key from Settings → SMTP & API>
   - `MAIL_FROM` = projectg5741@gmail.com (must be verified as a sender in Brevo)

   Brevo setup (one-time): sign up at https://www.brevo.com → **Senders** → add and verify `projectg5741@gmail.com` → **SMTP & API** → **API keys** tab → **Generate a new API key** (must start with `xkeysib-`, not `xsmtpsib-`).

   **Local dev** keeps `MAIL_PROVIDER=smtp` with `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `MAIL_USERNAME`, `MAIL_PASSWORD`.

   Password-reset links use the browser `Origin` when allowed (localhost or Vercel); otherwise `FRONTEND_URL` is the fallback base. Link shape: `https://oop-autograder.vercel.app?resetToken=...`
4. Memory: the API JVM and the isolated testcase worker share one cgroup. The image starts the API as `exec java $JAVA_OPTS -jar app.jar` with default `JAVA_OPTS=-Xmx192m` (plus Metaspace/code-cache caps and G1 periodic GC so idle heap can shrink). Do **not** set `JAVA_OPTS=-Xmx512m` — that leaves no room for the worker (`-Xmx64m -XX:MaxMetaspaceSize=48m -XX:+ExitOnOutOfMemoryError`). Defaults: `app.compile.parallelism=2`, `app.grading.parallelism=2`, `app.grading.worker-idle-ttl-ms=5000` (short clean warm park; dirty never park; set `WORKER_IDLE_TTL_MS=0` to close unless demand). On tight 512MB hosts you can set both to `1` via env. Measure peak RSS on a representative lab before raising the API heap. The image contains `/app/app.jar` and `/app/worker.jar` and sets `WORKER_JAR=/app/worker.jar`. **Do not** copy the local Maven value (`target/backend-1.0.0-worker.jar`) into Render env — that resolves to `/app/target/...` and breaks operational testcases. Leave `WORKER_JAR` unset (image default) or set `/app/worker.jar`. At most one worker JVM runs at a time. `JAVA_OPTS` does not apply to the worker.

   **RSS checklist (gate deploys):** (1) cold idle API RSS, no worker; (2) peak during one Official lab upload with OT; (3) RSS at +30s idle (worker gone; caches swept); (4) Metaspace after 10 sequential uploads. Fail the change if a 512MB host OOMs on the representative lab.
5. (Optional) Grading performance: `app.grading.rubric-cache-ttl-minutes` (default `2`, max size `8`), `app.grading.timing-log` (`true` to log upload phase timings including `compile_timing` per challenge). Multi-instance deployments need a shared cache (e.g. Redis) or accept per-instance TTL staleness until rubric invalidation is wired.
6. **Health check:** In the Render service **Settings** → **Health Checks**, set **Health Check Path** to `/` (not `/api/health`). The backend answers `GET /` with `200 ok`. Unknown paths return `404` without ERROR logs.
7. **Production — student deny on operational tests:** On the Render API set both:
   - `STUDENT_DENY_REQUIRED` = `true` (cloud boot fails if the policy flag is off)
   - `STUDENT_DENY_ENABLED` = `true`
   Upload, bulk grade, and lecturer dry-run then start the OT worker with a Java deny policy. A blocked student call fails that test with feedback `This call was blocked.` Compile and Class-tab load stay outside the policy. This PC and both desktop apps keep the open worker (`student-deny.enabled` forced false). There is no second host and no Docker runner. If the policy cannot be installed, that worker exits and the test is not run in the open process; the API stays up.
8. **Offline practice zip (Download practice folder):** Assemble on Windows (`scripts/assemble-student-desktop.ps1`) → `backend/target/OOP-AutoGrader-Practice.zip` (~160MB; do not commit). Maintain a **stable** GitHub Release tag (recommended `desktop-practice`) with only this zip; versioned archive releases (`v1.0.0`, `v1.0.1`, …) are optional history. Set GitHub Actions secret / Container App env `PRACTICE_BUNDLE_URL` to the stable asset URL (no `backend/.env` in production). After each stable-asset update, **restart** the Container App so startup prefetch pulls the new zip (**no image rebuild**). Optional: **`POST /api/lecturer/desktop-bundles/refresh`** (Admin JWT) without restart once downloads have finished once before. Unset URL and no local zip → student download returns 503.

   `https://github.com/Project-G5741/OOP-AutoGrader/releases/download/desktop-practice/OOP-AutoGrader-Practice.zip`

   Public URL, no token. Azure: `.github/workflows/deploy.yml` injects both bundle URLs from repository secrets on deploy.
9. **Offline lecturer zip (Bulk folder):** Assemble → `backend/target/OOP-AutoGrader-LecturerBulk.zip`. Stable tag `desktop-lecturer-bulk`; secret `LECTURER_BULK_BUNDLE_URL`. Same refresh options as step 8. Unset → Bulk folder returns 503.
10. Deploy. Check logs for successful startup.

Local image (omit either `--build-arg` to skip that zip; `mvn test` runs in the build stage):
```
cd backend
docker build --build-arg PRACTICE_BUNDLE_URL=<practice-asset-url> --build-arg LECTURER_BULK_BUNDLE_URL=<lecturer-asset-url> -t eiu-backend:latest .
docker run -e DB_USERNAME=<user> -e DB_PASSWORD=<pass> -e SPRING_DATASOURCE_URL="jdbc:postgresql://<host>:5432/<db>?sslmode=require" -e JWT_SECRET="<at-least-32-byte-secret>" -e PORT=8002 -p 8002:8002 eiu-backend:latest
```
