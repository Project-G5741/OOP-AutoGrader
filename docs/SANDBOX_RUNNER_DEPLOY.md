# Operational-test isolation on Render

The Docker `sandbox-runner` is not part of the live path. Render cannot host a nested container, and this project does not run a second VM.

Operational tests (student upload, lecturer bulk grade, lecturer dry-run) run in the local `worker.jar`. On Render set both:

| Env | Value |
|---|---|
| `STUDENT_DENY_REQUIRED` | `true` — cloud boot fails if the policy flag is off |
| `STUDENT_DENY_ENABLED` | `true` — OT worker installs the Java deny policy |

A blocked student call fails that test with feedback `This call was blocked.` Compile and Class-tab `--reflect` stay outside the policy. This PC and both desktop apps force `app.grading.student-deny.enabled=false` and keep the open worker.

The policy is a Java `SecurityManager` on Temurin 21. It is not a cgroup, network namespace, or read-only filesystem. Existing bounds stay: 3 seconds per test item, 64 MB worker heap. If the policy cannot be installed, that worker exits and the API does not run the test in the open process.

See `backend/DEPLOY_RENDER.md` step 7.
