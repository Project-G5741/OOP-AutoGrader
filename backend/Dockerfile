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
ENTRYPOINT ["java", "-jar", "app.jar"]
