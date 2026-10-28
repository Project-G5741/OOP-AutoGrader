Railway deployment (Docker)

Railway Hobby caps **Replica Limits** at **2 vCPU** and **1 GB** memory; the slider may not go below those plan maximums. Billing RAM is **usage × minutes** (minutely GB), not the 1 GB cap — Serverless scale-to-zero is the main cost lever when the SPA is idle.

The repo enforces a **small-host memory budget** in the image and Spring config so RSS stays near ~512 MB targets even when Railway allocates a 1 GB cgroup.

## Service setup

1. Connect the repo; set **Root Directory** to `backend` (Dockerfile build).
2. Enable **Serverless** (scale to zero). Avoid frequent health-check probes that prevent sleep; use `GET /` only if needed, with a long interval.
3. Copy env vars from `backend/.env.backend.example` (Neon pooler URL, JWT, Google, Brevo mail, `SPRINGDOC_ENABLED=false`, etc.). Same as [DEPLOY_RENDER.md](./DEPLOY_RENDER.md) except where noted below.

## Memory enforced in the repo (not the Railway slider)

| Knob | Where | Default |
|------|--------|---------|
| API heap + metaspace + code cache | `Dockerfile` `JAVA_OPTS` | `-Xmx256m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -XX:+ExitOnOutOfMemoryError` |
| Worker heap | `WorkerProcessClient` / `app.grading.worker-xmx` | `64m` (+ metaspace cap in code); does **not** use `JAVA_OPTS` |
| Compile / grading pools | `application.properties` | `app.compile.parallelism=2`, `app.grading.parallelism=2` (override via `APP_COMPILE_PARALLELISM` / `APP_GRADING_PARALLELISM`) |
| Hikari pool | `application.properties` | `HIKARI_MAXIMUM_POOL_SIZE` default **5** (raise to `10` locally if you need more concurrent DB work) |

Do **not** set `JAVA_OPTS=-Xmx512m` on Railway — the API and worker share one cgroup; upload peaks need headroom for a second JVM when operational testcases run.

After deploy, run one **worst-case student upload** (multi-challenge lab with operational testcases). If the replica OOMs, raise only `JAVA_OPTS` heap slightly or lower parallelism — do not remove metaspace caps without measuring.

## Frontend and wake traffic

The Vercel SPA records presence on mount, route change, focus, and visible tab — **not** on a fixed interval — so idle browsers do not keep Serverless awake. See `frontend/src/utils/presence.js`.

## Practice zip

Optional Docker build-arg `PRACTICE_BUNDLE_URL` — same as [DEPLOY_RENDER.md](./DEPLOY_RENDER.md) step 8.
