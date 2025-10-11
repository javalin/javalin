#!/bin/bash
# Javalin Development Server Helper Script
#
# Usage: ./dev-server.sh <MainClass> [app-args...]
# Example: ./dev-server.sh com.example.MyApp

set -e

if [ -z "$1" ]; then
    echo "Usage: $0 <MainClass> [app-args...]"
    echo "Example: $0 com.example.MyApp"
    exit 1
fi

MAIN_CLASS="$1"
shift
APP_ARGS="$@"

echo "Building classpath..."

# Build the project first
mvn compile -q

# Get dependencies classpath
mvn dependency:build-classpath -Dmdep.outputFile=.classpath -q

# Read classpath
CP=$(cat .classpath)

# Add target/classes
FULL_CP="target/classes:$CP"

echo "Starting Javalin Development Server..."
echo ""

# Run the dev server
java -cp "$FULL_CP" io.javalin.util.DevServer "$MAIN_CLASS" $APP_ARGS

# Cleanup
rm -f .classpath
