# ===== Stage 1: Build Java app =====
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy Maven project files for dependency caching
COPY pom.xml .
COPY src ./src

# Build the fat jar (skip tests for speed)
RUN mvn clean package -DskipTests

# ===== Stage 2: Production Linux Runtime =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy Go plugin (Linux binary) from your source tree
COPY go_executable/pluginengine /app/go_executable/pluginengine

# Make the Go plugin executable
RUN chmod +x /app/go_executable/pluginengine

# Expose HTTP port (adjust as needed for your app)
EXPOSE 8080

# Run your fat jar
ENTRYPOINT ["java", "-jar", "app.jar"]
