#!/bin/bash
set -e

echo "🚀 Starting Go plugin..."
# Start the Go plugin in the background
./go_executable/pluginengine &
GO_PID=$!

# Wait for 2 seconds before starting Java
sleep 2

echo "🚀 Starting Java Vert.x application..."
exec java -jar app.jar
