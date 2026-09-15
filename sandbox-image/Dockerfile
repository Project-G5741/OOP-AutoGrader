# Copy worker JAR into this directory before build:
#   cp ../backend/target/backend-1.0.0-worker.jar .
FROM eclipse-temurin:21-jre-jammy

RUN groupadd -r sandbox && useradd -r -g sandbox -d /opt/worker sandbox

WORKDIR /opt/worker
COPY backend-1.0.0-worker.jar /opt/worker/worker.jar
RUN chown -R sandbox:sandbox /opt/worker

USER sandbox

ENTRYPOINT ["java", "-Xmx64m", "-XX:MaxMetaspaceSize=48m", "-XX:+ExitOnOutOfMemoryError", "-jar", "/opt/worker/worker.jar"]
