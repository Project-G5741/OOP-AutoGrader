# Sandbox runner deployment

Stage-3 **container sandbox** for operational testcase invoke only. The Render API delegates to a dedicated `sandbox-runner` service over authenticated HTTP; the API image does **not** mount the Docker socket.

## Architecture

```
Render API (app.jar)
  └─ WorkerSessionFactory (app.grading.sandbox.enabled=true)
       └─ RemoteWorkerSessionClient → POST /sessions (tar.gz classes)
            └─ sandbox-runner VM (Docker)
                 └─ warm pool of hardened containers (network none, read-only root, tmpfs /work)
                      └─ worker.jar NDJSON IPC
```

## VM recommendation

- **Hetzner CX22** or **DigitalOcean Basic 2 vCPU / 4 GB** — enough for min pool 1–2 containers plus runner JVM.
- Install Docker Engine and enable TLS or restrict runner port to API egress IP.
- Put **Caddy** or nginx in front of `sandbox-runner:8090` with TLS.

## Build artifacts

```bash
# Worker JAR (required for sandbox image)
mvn -f backend/pom.xml package -DskipTests
cp backend/target/backend-1.0.0-worker.jar sandbox-image/

# Sandbox worker image
docker build -t oop-autograder-sandbox-worker:latest sandbox-image/

# Runner service
mvn -f sandbox-runner/pom.xml package -DskipTests
docker build -t oop-autograder-sandbox-runner:latest sandbox-runner/
```

## Runner environment

| Variable | Purpose |
|---|---|
| `SANDBOX_RUNNER_TOKEN` | Bearer token shared with API (`SANDBOX_RUNNER_TOKEN`) |
| `SANDBOX_IMAGE` | Worker image name (default `oop-autograder-sandbox-worker:latest`) |
| `SANDBOX_POOL_MIN` | Minimum idle containers (default `1`) |
| `SANDBOX_POOL_MAX` | Maximum live containers (default `4`) |
| `SANDBOX_SESSION_TTL_SECONDS` | Idle session TTL before sweep (default `600`) |
| `SANDBOX_MAX_SESSIONS` | Max concurrent sessions (default `64`) |
| `SANDBOX_SESSION_SWEEP_MS` | Sweep interval in ms (default `60000`) |
| `PORT` | HTTP port (default `8090`) |

`GET /health` is public. All `/sessions/**` routes require `Authorization: Bearer <token>`.

## API environment (Render)

| Variable | Purpose |
|---|---|
| `SANDBOX_ENABLED` | `true` to route testcase invoke/dry-run through runner |
| `SANDBOX_RUNNER_URL` | HTTPS base URL, e.g. `https://sandbox.example.com` |
| `SANDBOX_RUNNER_TOKEN` | Same secret as runner |

When `SANDBOX_ENABLED=true`, the API does **not** fall back to the local `worker.jar` on runner failure (infrastructure ERROR).

## Local stack

```bash
export SANDBOX_RUNNER_TOKEN=dev-sandbox-token
docker compose -f docker-compose.sandbox.yml up --build
```

In `backend/.env`:

```
SANDBOX_ENABLED=true
SANDBOX_RUNNER_URL=http://localhost:8090
SANDBOX_RUNNER_TOKEN=dev-sandbox-token
```

## Integration tests

```bash
export SANDBOX_INTEGRATION=true
export SANDBOX_RUNNER_URL=http://localhost:8090
export SANDBOX_RUNNER_TOKEN=dev-sandbox-token
mvn -f backend/pom.xml test -Dtest=ContainerSandboxAeTest
```

## Latency baseline (KTD12)

Record upload `[timing]` `worker_spawn_ms` and total grade time for a reference lab before enabling sandbox, then again with `SANDBOX_ENABLED=true` and a warm pool. Expect higher first-session cost; steady-state should approach stage-2 when pool is warm.
