# Stage 1: Build
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /build

# Copy pom.xml và source code
COPY pom.xml .
COPY src ./src

# Chạy Maven build (không cần mvnw)
RUN mvn -B test package

# Stage 2: Runtime
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copy API fat JAR and thin worker JAR by explicit names (glob would be unsafe with two artifacts).
COPY --from=build /build/target/backend-1.0.0.jar app.jar
COPY --from=build /build/target/backend-1.0.0-worker.jar worker.jar

# Optional bake at build time (legacy). Cloud prefers runtime PRACTICE_BUNDLE_URL /
# LECTURER_BULK_BUNDLE_URL — DesktopBundlePrefetchRunner on startup. See DEPLOY_RENDER.md.
ARG PRACTICE_BUNDLE_URL=""
ARG LECTURER_BULK_BUNDLE_URL=""
RUN set -eu; \
    if [ -z "$PRACTICE_BUNDLE_URL" ] && [ -z "$LECTURER_BULK_BUNDLE_URL" ]; then \
      echo "Bundle URLs unset; practice and lecturer zips will be absent"; \
    else \
      export DEBIAN_FRONTEND=noninteractive; \
      apt-get update; \
      apt-get install -y --no-install-recommends curl ca-certificates; \
      fetch() { \
        name="$1"; url="$2"; \
        if [ -z "$url" ]; then return 0; fi; \
        curl -fL --retry 3 --retry-delay 2 -o "/app/$name" "$url" || exit 1; \
        magic=$(od -An -tx1 -N2 "/app/$name" | tr -d ' \t\n'); \
        [ "$magic" = "504b" ] || exit 1; \
        [ "$(stat -c%s "/app/$name")" -gt 1000000 ] || exit 1; \
      }; \
      fetch OOP-AutoGrader-Practice.zip "$PRACTICE_BUNDLE_URL"; \
      fetch OOP-AutoGrader-LecturerBulk.zip "$LECTURER_BULK_BUNDLE_URL"; \
      apt-get purge -y curl; \
      rm -rf /var/lib/apt/lists/*; \
    fi

EXPOSE 8080
# API JVM caps (logical ~512MB budget with worker -Xmx64m in the same cgroup). Worker does not inherit JAVA_OPTS.
# Railway Hobby may still show a 1GB replica limit; these flags keep RSS closer to small-host targets.
ENV JAVA_OPTS="-Xmx192m -XX:MaxMetaspaceSize=96m -XX:ReservedCodeCacheSize=32m -XX:+ExitOnOutOfMemoryError -XX:G1PeriodicGCInterval=15000 -XX:G1PeriodicGCSystemLoadThreshold=0"
# Image layout path. Do not override on Render with the local Maven path (target/...).
# WorkerProcessClient also falls back to /app/worker.jar if a stale WORKER_JAR is set.
ENV WORKER_JAR=/app/worker.jar
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
