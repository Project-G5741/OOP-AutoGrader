# Sandbox worker image

Runs `backend-1.0.0-worker.jar` for operational testcase invoke inside an isolated container.

## Build

```bash
cd backend && mvn -q -DskipTests package
docker build -f sandbox-image/Dockerfile -t oop-autograder-sandbox-worker:latest sandbox-image \
  --build-context backend=backend/target
```

Or copy the worker JAR into this directory as `backend-1.0.0-worker.jar` and:

```bash
docker build -t oop-autograder-sandbox-worker:latest sandbox-image
```

## Runtime flags (applied by sandbox-runner)

The runner creates containers with:

- `--network none`
- Read-only root filesystem
- tmpfs at `/work` for extracted student classes
- `--memory 128m`, `--cpus 0.5`, `--pids-limit 64`
- Non-root `sandbox` user (image default)
