# Use official Maven image to build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Use JRE-only image for runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/my-vertx-project-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
