Render deployment steps (Docker)

1. In Render dashboard, create a new Web Service -> "Docker" or "Connect a repo" and select this repository.
2. Set the root directory to `backend` (Render will use the Dockerfile in that folder).
3. Environment variables to add:
   - `SPRING_DATASOURCE_URL` = `jdbc:postgresql://<POOLER-HOST>-pooler.<region>.aws.neon.tech:5432/<DB_NAME>?sslmode=require` — use the **pooler** hostname (`-pooler` in the host) for long-lived JVM deployments on Render. See `backend/.env.backend.example` for a working Neon pooler URL. The direct (non-pooler) endpoint can exhaust Neon connection limits with default Hikari pool size (~10).
   - `DB_USERNAME` = <db user>
   - `DB_PASSWORD` = <db pass>
   - `FRONTEND_URL` = https://oop-autograder.vercel.app
   - `JWT_SECRET` = <your-jwt-secret, at least 32 bytes; generate with `openssl rand -base64 32`. Changing this invalidates all existing sessions.>
   - `GOOGLE_CLIENT_ID` = <google client id>
   - `SPRINGDOC_ENABLED` = `false` (do not register OpenAPI/Swagger in production)

   **Password-reset email (Render free tier):** Render blocks outbound SMTP (ports 25/465/587). Use the Brevo HTTPS API instead of Gmail SMTP:
   - `MAIL_PROVIDER` = `brevo`
   - `BREVO_API_KEY` = <Brevo API key from Settings → SMTP & API>
   - `MAIL_FROM` = projectg5741@gmail.com (must be verified as a sender in Brevo)

   Brevo setup (one-time): sign up at https://www.brevo.com → **Senders** → add and verify `projectg5741@gmail.com` → **SMTP & API** → **API keys** tab → **Generate a new API key** (must start with `xkeysib-`, not `xsmtpsib-`).

   **Local dev** keeps `MAIL_PROVIDER=smtp` with `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `MAIL_USERNAME`, `MAIL_PASSWORD`.

   Password-reset links use the browser `Origin` when allowed (localhost or Vercel); otherwise `FRONTEND_URL` is the fallback base. Link shape: `https://oop-autograder.vercel.app?resetToken=...`
4. Memory: the API JVM and the isolated testcase worker share one cgroup. Do **not** set `JAVA_OPTS=-Xmx512m` on the worker-enabled image — that leaves no room for the worker (`-Xmx64m -XX:MaxMetaspaceSize=48m -XX:+ExitOnOutOfMemoryError`). Pair a lower API heap (start with `-Xmx256m`, then measure peak RSS on a representative lab) with those worker flags. On 512MB hosts set `app.compile.parallelism=2` and `app.grading.parallelism=2`. The image contains `/app/app.jar` and `/app/worker.jar`; override worker path with `WORKER_JAR` only if you move the file. At most one worker JVM runs at a time.
5. (Optional) Grading performance: `app.grading.rubric-cache-ttl-minutes` (default `30`), `app.grading.timing-log` (`true` to log upload phase timings including `compile_timing` per challenge). Multi-instance deployments need a shared cache (e.g. Redis) or accept per-instance TTL staleness until rubric invalidation is wired.
6. **Health check:** In the Render service **Settings** → **Health Checks**, set **Health Check Path** to `/` (not `/api/health`). The backend answers `GET /` with `200 ok`. Unknown paths return `404` without ERROR logs.
7. Deploy. Check logs for successful startup.

Local build & test (image build runs `mvn test`; no database env vars are required for that step):
```
cd backend
docker build -t eiu-backend:latest .
docker run -e DB_USERNAME=<user> -e DB_PASSWORD=<pass> -e SPRING_DATASOURCE_URL="jdbc:postgresql://<host>:5432/<db>?sslmode=require" -e JWT_SECRET="<at-least-32-byte-secret>" -e PORT=8002 -p 8002:8002 eiu-backend:latest
```
