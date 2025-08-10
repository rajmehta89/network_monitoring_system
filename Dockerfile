# ===== Stage 1: Build Go Plugin =====
FROM golang:1.21 AS go-build

RUN apt-get update && apt-get install -y --no-install-recommends libzmq3-dev pkg-config && rm -rf /var/lib/apt/lists/*
WORKDIR /go-plugin

# Copy Go modules and download dependencies
COPY pluginengine/go.mod pluginengine/go.sum ./
RUN go mod download

# Copy Go source code files
COPY pluginengine/. .
RUN go build -o pluginengine main.go

# ===== Stage 2: Build Java Application =====
FROM maven:3.9.6-eclipse-temurin-21 AS java-build
WORKDIR /java-app

COPY pom.xml .
COPY src ./src

# Build fat jar with Maven (skip tests for speed)
RUN mvn clean package -DskipTests

# ===== Stage 3: Runtime Image =====
FROM eclipse-temurin:21-jre

# Install ZeroMQ runtime dependency for Go plugin
RUN apt-get update && apt-get install -y --no-install-recommends libzmq5 curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# ---- Add CA cert for Render PostgreSQL ----
RUN mkdir -p /app/certs \
    && curl -o /app/certs/isrgrootx1.pem https://letsencrypt.org/certs/isrgrootx1.pem

# Copy the Java fat jar (should be shaded with correct manifest)
COPY --from=java-build /java-app/target/my-vertx-project-1.0-SNAPSHOT.jar app.jar

# Copy the Go plugin binary
COPY --from=go-build /go-plugin/pluginengine go_executable/pluginengine

RUN chmod +x go_executable/pluginengine && strip go_executable/pluginengine 2>/dev/null || true

# Copy startup script and make it executable
COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh

EXPOSE 8080

ENTRYPOINT ["/app/start.sh"]
