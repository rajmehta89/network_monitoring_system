# ===== Stage 1: Build Go Plugin =====
FROM golang:1.21 AS go-build
RUN apt-get update && apt-get install -y --no-install-recommends libzmq3-dev pkg-config && rm -rf /var/lib/apt/lists/*
WORKDIR /go-plugin
COPY pluginengine/go.mod pluginengine/go.sum ./
RUN go mod download
COPY pluginengine/. .
RUN go build -o pluginengine main.go

# ===== Stage 2: Build Java Application =====
FROM maven:3.9.6-eclipse-temurin-21 AS java-build
WORKDIR /java-app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ===== Stage 3: Runtime Image =====
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends libzmq5 curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
RUN mkdir -p /app/certs \
    && curl -o /app/certs/isrgrootx1.pem https://letsencrypt.org/certs/isrgrootx1.pem
COPY --from=java-build /java-app/target/my-vertx-project-1.0-SNAPSHOT.jar app.jar
COPY --from=go-build /go-plugin/pluginengine go_executable/pluginengine
RUN chmod +x go_executable/pluginengine && strip go_executable/pluginengine 2>/dev/null || true
COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh

# Expose web + ZMQ ports
EXPOSE 8080 5555 5556

ENTRYPOINT ["/app/start.sh"]
