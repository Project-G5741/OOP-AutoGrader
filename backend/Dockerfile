# Stage 1: Build
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /build

# Copy pom.xml và source code
COPY pom.xml .
COPY src ./src

# Chạy Maven build (không cần mvnw)
RUN mvn -B test package

# Stage 2: Runtime
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copy API fat JAR and thin worker JAR by explicit names (glob would be unsafe with two artifacts).
COPY --from=build /build/target/backend-1.0.0.jar app.jar
COPY --from=build /build/target/backend-1.0.0-worker.jar worker.jar

EXPOSE 8080
# API heap only. The testcase worker is spawned with hardcoded -Xmx64m and does not inherit JAVA_OPTS.
ENV JAVA_OPTS="-Xmx256m"
# Image layout path. Do not override on Render with the local Maven path (target/...).
# WorkerProcessClient also falls back to /app/worker.jar if a stale WORKER_JAR is set.
ENV WORKER_JAR=/app/worker.jar
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
