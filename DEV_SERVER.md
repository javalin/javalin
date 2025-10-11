# Javalin Development Server

A command-line tool that provides automatic compilation and server restart for Javalin applications during development.

## Overview

The Javalin Development Server watches your source files for changes, automatically compiles them using Maven, and restarts your application - all without requiring any code changes to your application.

## Quick Start

### 1. Build your project once

```bash
mvn package
```

### 2. Run your app with the dev server

```bash
java -cp "target/classes:target/dependency/*:path/to/javalin.jar" \
  io.javalin.util.DevServer \
  com.example.MyApp
```

Or create a simple script:

```bash
#!/bin/bash
# dev.sh
mvn dependency:build-classpath -Dmdep.outputFile=.classpath -q
CP=$(cat .classpath)
java -cp "target/classes:$CP" io.javalin.util.DevServer com.example.MyApp
```

Then just run:
```bash
chmod +x dev.sh
./dev.sh
```

### 3. Make changes and save

The dev server will automatically:
1. Detect your changes
2. Compile your code with `mvn compile`
3. Restart your application

## How It Works

```
┌─────────────────────────────────────────────────────────────┐
│  1. You edit and save a .java/.kt/.properties file         │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  2. Dev Server detects the change                          │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  3. Dev Server runs: mvn compile                           │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  4. Dev Server kills the running application               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  5. Dev Server starts a new instance with updated classes  │
└─────────────────────────────────────────────────────────────┘
```

## Features

✅ **Zero code changes** - Your application code remains completely unchanged  
✅ **Automatic compilation** - Runs `mvn compile` when changes detected  
✅ **Full restart** - Kills and restarts the JVM process  
✅ **Recursive watching** - Watches all subdirectories in src/main/  
✅ **Smart debouncing** - Waits for you to finish editing before restarting  
✅ **Maven integration** - Uses Maven for compilation and classpath  

## Usage

### Basic Usage

```bash
java -cp <classpath> io.javalin.util.DevServer <MainClass> [app-args...]
```

**Parameters:**
- `<MainClass>` - Your application's main class (e.g., `com.example.MyApp`)
- `[app-args...]` - Optional arguments to pass to your application

### Example Application

```java
package com.example;

import io.javalin.Javalin;

public class MyApp {
    public static void main(String[] args) {
        Javalin.create()
            .get("/", ctx -> ctx.result("Hello World"))
            .start(7070);
    }
}
```

**No changes needed!** Just run it with DevServer:

```bash
java -cp "target/classes:..." io.javalin.util.DevServer com.example.MyApp
```

## What It Watches

The dev server watches these directories for changes:
- `src/main/java/**/*.java`
- `src/main/kotlin/**/*.kt`
- `src/main/resources/**/*.properties`
- `src/main/resources/**/*.xml`
- `src/main/resources/**/*.yaml`
- `src/main/resources/**/*.yml`
- `src/main/resources/**/*.json`

## Requirements

- Maven project structure (`pom.xml`, `src/main/java`, etc.)
- Maven command (`mvn`) available in PATH
- Java 17+ (same as Javalin)
- Application must have a main class that starts a Javalin server

## Helper Scripts

### Bash Script (dev.sh)

```bash
#!/bin/bash
# Get dependencies classpath
mvn dependency:build-classpath -Dmdep.outputFile=.classpath -q

# Read classpath
CP=$(cat .classpath)

# Find Javalin JAR in classpath
JAVALIN_JAR=$(echo $CP | tr ':' '\n' | grep javalin | head -1)

# Run dev server
java -cp "target/classes:$CP" io.javalin.util.DevServer com.example.MyApp "$@"
```

### Maven Exec Plugin

Add to your `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>exec-maven-plugin</artifactId>
            <version>3.1.0</version>
            <configuration>
                <mainClass>io.javalin.util.DevServer</mainClass>
                <arguments>
                    <argument>com.example.MyApp</argument>
                </arguments>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Then run:
```bash
mvn exec:java
```

## Comparison with Other Approaches

| Approach | Code Changes | Compilation | Restart | Build Tool |
|----------|--------------|-------------|---------|------------|
| DevServer | None | Automatic | Full | Maven only |
| JavalinDevMode (old) | Wrapper needed | Manual | Full | Any |
| JRebel | Agent | N/A | None | Any |
| Spring DevTools | Framework-specific | Automatic | Full | Any |

## Troubleshooting

### "mvn: command not found"

Make sure Maven is installed and available in your PATH:
```bash
mvn --version
```

### "No source directories found to watch"

Make sure you're running from your Maven project root directory (where `pom.xml` is located).

### Application doesn't restart

1. Check that Maven compilation succeeded (look for compilation output)
2. Verify your main class name is correct
3. Check for compilation errors in the console

### Port already in use

If the old process didn't stop cleanly, wait a few seconds or kill it manually:
```bash
# Find process using port 7070
lsof -i :7070
# Kill it
kill <PID>
```

## Tips

- Use `Ctrl+C` to stop the dev server
- The server waits 1 second after last change before restarting (debouncing)
- Compilation errors won't crash the dev server - fix them and save again
- The dev server shows all compilation output, making errors easy to spot

## Limitations

- **Maven only** - Currently only supports Maven projects (Gradle support planned)
- **Full restart** - Restarts the entire JVM (no hot class swapping)
- **State loss** - Application state is lost on restart
- **Slower than hot-swap** - But much simpler and more reliable

## Example Output

```
======================================================================
Javalin Development Server
======================================================================
Main class: com.example.MyApp
Watching: src/main/java, src/main/resources
Press Ctrl+C to stop
======================================================================

[DevServer] Compiling...
[DevServer] Compilation successful
[DevServer] Starting application: com.example.MyApp
[DevServer] Application started (PID: 12345)
[DevServer] Watching: src/main/java

[DevServer] Detected change: src/main/java/com/example/MyApp.java

======================================================================
Changes detected - recompiling and restarting...
======================================================================
[DevServer] Stopping application...
[DevServer] Application stopped
[DevServer] Compiling...
[DevServer] Compilation successful
[DevServer] Starting application: com.example.MyApp
[DevServer] Application started (PID: 12346)
```

## Related

- [Javalin Documentation](https://javalin.io/documentation)
- [Maven Documentation](https://maven.apache.org/)
