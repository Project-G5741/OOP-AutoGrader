# Isolated testcase worker

## Purpose

Process entry for operational testcase invoke. Runs in a separate JVM from the API.

## Ownership

| File | Role |
|---|---|
| `WorkerMain.java` | `main`; retains stdin/stdout for IPC before any `System.setOut` |
| `WorkerIpc.java` | NDJSON ops `invoke`, `compare`, `scenario`; request/response records |
| `WorkerInvokeEngine.java` | Reflection invoke, two-instance compare, and named-instance scenario |

## Local Contracts

- Launch with `java -jar worker.jar`, never the API fat JAR or `PropertiesLauncher`.
- `--self-check` prints `{"ok":true}` and exits (packaging probe).
- Probe flags `--dump-env`, `--hang`, `--child-hang`, `--stderr-flood` skip the IPC loop.
- Default path is the NDJSON IPC loop (`WorkerIpc` → `WorkerInvokeEngine`). Student classes load only in this JVM.
- IPC stdout is UTF-8 NDJSON; one request line in, one response line out. The API must decode those bytes as UTF-8.
- Worker JAR must not contain `org.springframework` types.
- Ops: `invoke` (one constructor/method), `compare` (two instances), `scenario` (ordered steps sharing a request-local name → live object registry on one ClassLoader).
- `scenario` looks up a method on `dispatchClassName` when set, then `invoke`s it on the named receiver so dynamic dispatch runs. Without dispatch, keep concrete-class `getDeclaredMethod` lookup.
- Params may include `{"$instance":"<name>"}`; live objects never round-trip as JSON.
- CONSTRUCTOR `THREW`, `ERROR`, or `TIMED_OUT`: omit later steps. METHOD `THREW`: keep the named receiver in the registry and continue later steps. Completed scenario top-level `kind` is `NORMAL` (facts in `steps`) or `ERROR`/`TIMED_OUT` for whole-op failure.
- Whole-scenario timeout is the existing session `readLine` budget (`app.grading.testcase-invoke-timeout-seconds`); one round-trip, no per-step kill/respawn.
- Never emit `passed`. Wire `kind` is a string (`KIND_NORMAL`), not the API enum.

## Work Guidance

Keep this package off the student classloader URL list.

## Verification

`WorkerJarIsolationTest`; `WorkerInvokeEngineTest`; `java -jar target/backend-1.0.0-worker.jar --self-check`.

## Child DOX Index

No child docs.
