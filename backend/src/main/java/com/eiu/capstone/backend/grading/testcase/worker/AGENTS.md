# Isolated testcase worker

## Purpose

Process entry for operational testcase invoke. Runs in a separate JVM from the API.

## Ownership

| File | Role |
|---|---|
| `WorkerMain.java` | `main`; retains stdin/stdout for IPC before any `System.setOut` |

## Local Contracts

- Launch with `java -jar worker.jar`, never the API fat JAR or `PropertiesLauncher`.
- `--self-check` prints `{"ok":true}` and exits (packaging probe).
- NDJSON protocol is filled by the invoke engine; this class must not load student classes.
- Worker JAR must not contain `org.springframework` types.

## Work Guidance

Keep this package off the student classloader URL list.

## Verification

`WorkerJarIsolationTest`; `java -jar target/backend-1.0.0-worker.jar --self-check`.

## Child DOX Index

No child docs.
