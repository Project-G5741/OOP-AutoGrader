# Isolated testcase worker

## Purpose

Process entry for operational testcase invoke. Runs in a separate JVM from the API.

## Ownership

| File | Role |
|---|---|
| `WorkerMain.java` | `main`; retains stdin/stdout for IPC before any `System.setOut` |
| `WorkerIpc.java` | NDJSON ops `invoke`, `scenario`, `batch`; request/response records |
| `WorkerInvokeEngine.java` | Reflection invoke and named-instance scenario |

## Local Contracts

- Launch with `java -jar worker.jar`, never the API fat JAR or `PropertiesLauncher`.
- `--self-check` prints `{"ok":true}` and exits (packaging probe).
- Probe flags `--dump-env`, `--hang`, `--child-hang`, `--stderr-flood` skip the IPC loop.
- Default path is the NDJSON IPC loop (`WorkerIpc` → `WorkerInvokeEngine`). Student classes load only in this JVM.
- IPC stdout is UTF-8 NDJSON; one request line in, one response line out. The API must decode those bytes as UTF-8.
- Worker JAR must not contain `org.springframework` types.
- Ops: `invoke` (one constructor/method), `scenario` (ordered steps sharing a request-local name → live object registry on one ClassLoader), `batch` (ordered list of scenarios for one challenge; one student `URLClassLoader` and one timeout executor for the whole batch; each item still gets a fresh named-instance registry). `compare` is unused.
- `scenario` looks up a method on `dispatchClassName` when set, then `invoke`s it on the named receiver so dynamic dispatch runs. Without dispatch, keep concrete-class `getDeclaredMethod` lookup.
- Params may include `{"$instance":"<name>"}`; live objects never round-trip as JSON.
- Constructor results and static-factory object returns register `instanceName`. Instance-method returns do not overwrite the named receiver.
- Any step `THREW`, `ERROR`, or `TIMED_OUT`: omit later steps. Completed scenario top-level `kind` is `NORMAL` (facts in `steps`) or `ERROR`/`TIMED_OUT` for whole-op failure.
- Worker facts for object checks: `objectTypeSimpleName`, `objectFieldSnapshotsJson` (one-level literals), `equalsNamed` (this object vs other registry names).
- Whole-scenario / per-batch-item execution timeout is enforced inside the worker (`Future.get` on `timeoutSeconds` from the request, shared batch executor). On hang timeout the worker **stops the batch**, writes the partial response, then `Runtime.halt(0)` so interrupt-immune tight loops die with the process. The API transport wait is `itemCount × timeoutSeconds + 30s` return slack; after a timeout abort it respawns (local, or remote reopen) and continues remaining items.
- Never emit `passed`. Wire `kind` is a string (`KIND_NORMAL`), not the API enum.

## Work Guidance

Keep this package off the student classloader URL list.

## Verification

`WorkerJarIsolationTest`; `WorkerInvokeEngineTest`; `java -jar target/backend-1.0.0-worker.jar --self-check`.

## Child DOX Index

No child docs.
