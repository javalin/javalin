#!/bin/bash
# Javalin Development Server
#
# Ultra-simple dev server - just run: ./dev-server.sh
# Automatically finds your main class and starts dev mode with hot reload.

set -e

echo "======================================================================"
echo "Javalin Development Server"
echo "======================================================================"
echo ""

# Check if main class was provided as argument
if [ -n "$1" ]; then
    MAIN_CLASS="$1"
    shift
    APP_ARGS="$@"
    echo "Using provided main class: $MAIN_CLASS"
else
    echo "Auto-detecting main class..."
    
    # Try to find main class from pom.xml first
    MAIN_CLASS=$(grep -A 5 "<plugin>" pom.xml 2>/dev/null | grep -A 3 "maven-jar-plugin\|exec-maven-plugin" | grep "<mainClass>" | head -1 | sed 's/.*<mainClass>\(.*\)<\/mainClass>.*/\1/' || echo "")
    
    # If not found in pom.xml, search for classes with main method
    if [ -z "$MAIN_CLASS" ]; then
        echo "Searching for classes with main method..."
        
        # Compile first to ensure classes are available
        mvn compile -q 2>/dev/null || true
        
        # Find all .class files with main method
        MAIN_CLASSES=$(find target/classes -name "*.class" -type f 2>/dev/null | while read -r classfile; do
            # Convert path to class name
            classname=$(echo "$classfile" | sed 's|target/classes/||' | sed 's|/|.|g' | sed 's|.class$||')
            # Check if class has main method
            if javap -cp target/classes "$classname" 2>/dev/null | grep -q "public static void main(java.lang.String"; then
                echo "$classname"
            fi
        done)
        
        # Count how many main classes we found
        MAIN_COUNT=$(echo "$MAIN_CLASSES" | grep -v '^$' | wc -l)
        
        if [ "$MAIN_COUNT" -eq 0 ]; then
            echo ""
            echo "ERROR: No main class found!"
            echo ""
            echo "Please either:"
            echo "  1. Specify main class: ./dev-server.sh com.example.YourApp"
            echo "  2. Add mainClass to your pom.xml (exec-maven-plugin or maven-jar-plugin)"
            echo ""
            exit 1
        elif [ "$MAIN_COUNT" -eq 1 ]; then
            MAIN_CLASS=$(echo "$MAIN_CLASSES" | tr -d '[:space:]')
            echo "Found main class: $MAIN_CLASS"
        else
            echo ""
            echo "Multiple main classes found:"
            echo "$MAIN_CLASSES" | grep -v '^$' | nl
            echo ""
            echo "Please specify which one to use:"
            echo "  ./dev-server.sh <MainClass>"
            echo ""
            echo "Or add mainClass to your pom.xml"
            exit 1
        fi
    else
        echo "Found main class in pom.xml: $MAIN_CLASS"
    fi
    
    APP_ARGS=""
fi

echo ""
echo "Main class: $MAIN_CLASS"
echo "Building classpath..."

# Get dependencies classpath
mvn dependency:build-classpath -Dmdep.outputFile=.classpath -q

# Read classpath
CP=$(cat .classpath)

# Add target/classes
FULL_CP="target/classes:$CP"

echo "Starting dev server with hot reload..."
echo "Edit your files and save - changes will auto-reload!"
echo "Press Ctrl+C to stop"
echo ""
echo "======================================================================"
echo ""

# Run the dev server
java -cp "$FULL_CP" io.javalin.util.DevServer "$MAIN_CLASS" $APP_ARGS

# Cleanup
rm -f .classpath
