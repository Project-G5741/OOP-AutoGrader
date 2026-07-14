# Stage 1: Build
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /build

# Copy pom.xml và source code
COPY pom.xml .
COPY src ./src

# Chạy Maven build (không cần mvnw)
RUN mvn -B -DskipTests package

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy JAR từ stage build
COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]