# Javalin Development Server

A command-line tool that provides automatic compilation and server restart for Javalin applications during development.

## Overview

The Javalin Development Server watches your source files for changes, automatically compiles them using Maven, and restarts your application - all without requiring any code changes to your application.

## Quick Start

### Just run the script!

```bash
./dev-server.sh
```

That's it! The script will:
1. Automatically find your main class (from pom.xml or by searching)
2. Build your classpath
3. Start your app with hot reload enabled
4. Watch for changes and automatically restart

### What you'll see

```bash
$ ./dev-server.sh
======================================================================
Javalin Development Server
======================================================================

Auto-detecting main class...
Found main class: com.example.MyApp

Main class: com.example.MyApp
Building classpath...
Starting dev server with hot reload...
Edit your files and save - changes will auto-reload!
Press Ctrl+C to stop
======================================================================
```

### First time setup

Make sure you have a Maven project with Javalin:

```bash
mvn compile  # First time only
```

### Make changes and save

The dev server will automatically:
1. Detect your changes
2. Compile your code with `mvn compile`
3. Restart your application

No manual restart needed!

## Advanced Usage

### Specify main class manually (optional)

If you have multiple main classes or want to specify which one to use:

```bash
./dev-server.sh com.example.MyApp
```

### Pass arguments to your application

```bash
./dev-server.sh com.example.MyApp arg1 arg2
```

### How main class detection works

The script tries to find your main class in this order:

1. **Command line argument** - If you provide it as first argument
2. **pom.xml** - Looks for `<mainClass>` in exec-maven-plugin or maven-jar-plugin
3. **Auto-scan** - Searches for classes with `public static void main` method

If multiple main classes are found, the script will list them and ask you to choose.

## Setting up main class in pom.xml (recommended)

Add this to your `pom.xml` for automatic detection:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>exec-maven-plugin</artifactId>
            <version>3.1.0</version>
            <configuration>
                <mainClass>com.example.MyApp</mainClass>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Then just run `./dev-server.sh` with no arguments!

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

✅ **One command to start** - Just run `./dev-server.sh` with no arguments  
✅ **Auto-detects main class** - Finds your app automatically  
✅ **Zero code changes** - Your application code remains completely unchanged  
✅ **Automatic compilation** - Runs `mvn compile` when changes detected  
✅ **Full restart** - Kills and restarts the JVM process  
✅ **Recursive watching** - Watches all subdirectories in src/main/  
✅ **Smart debouncing** - Waits for you to finish editing before restarting  
✅ **Maven integration** - Uses Maven for compilation and classpath  

## Manual Usage (Advanced)

If you want to bypass the script and run directly:

```bash
java -cp <classpath> io.javalin.util.DevServer <MainClass> [app-args...]
```

**Parameters:**
- `<MainClass>` - Your application's main class (e.g., `com.example.MyApp`)
- `[app-args...]` - Optional arguments to pass to your application

## Example Application

Your application needs no special code - just a regular Javalin app:

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

To run with dev server:
```bash
./dev-server.sh
```

That's it! Edit `MyApp.java`, save, and watch it automatically restart.

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
