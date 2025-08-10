#!/bin/bash
set -e

PORT=${PORT:-8080}  # fallback if PORT not set

echo "🚀 Starting Go plugin..."
./go_executable/pluginengine &
GO_PID=$!

echo "🚀 Starting Java Vert.x application on port $PORT..."
exec java -jar app.jar
