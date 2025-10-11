# JavalinDevMode - Development Mode Utility

The `JavalinDevMode` utility provides automatic server restart during development with minimal configuration.

## Overview

Unlike traditional hot reload solutions that require JVM agents or complex setup, `JavalinDevMode` provides a simple, practical approach to development-time server restart. When you make changes to your code, the utility automatically detects the recompiled classes and restarts your Javalin server.

## Quick Start

### 1. Update your main method

```java
public class MyApp {
    public static void main(String[] args) {
        JavalinDevMode.runWithAutoRestart(() -> {
            return Javalin.create(config -> {
                // your configuration
            })
            .get("/", ctx -> ctx.result("Hello World"))
            .start(7070);
        });
    }
}
```

### 2. Run continuous compilation

In a separate terminal, run your build tool in continuous mode:

**Maven:**
```bash
mvn compile -Ddev
```

**Gradle:**
```bash
gradle classes --continuous
```

### 3. Start your app

```bash
mvn exec:java -Dexec.mainClass="com.example.MyApp"
# or
java -cp target/classes com.example.MyApp
```

That's it! Now when you save changes to your source files, the build tool will recompile them, and JavalinDevMode will automatically restart your server.

## How It Works

1. **Automatic Directory Detection**: Detects Maven (`target/classes`) or Gradle (`build/classes`) class directories
2. **File Watching**: Monitors `.class` files for changes using Java NIO WatchService  
3. **Automatic Restart**: When changes detected, stops current server and starts a new one
4. **JVM Keep-Alive**: Uses a non-daemon thread to keep the JVM running between restarts
5. **Debouncing**: Waits for compilation to finish before restarting (500ms default)

## Features

- ✅ **Zero configuration** - No setup required, works out of the box
- ✅ **Cross-platform** - Uses standard Java NIO, works on all platforms
- ✅ **Build-tool agnostic** - Works with Maven, Gradle, or any other build tool
- ✅ **Smart detection** - Automatically finds your class directories
- ✅ **Graceful restart** - Properly stops old server before starting new one
- ✅ **Non-blocking** - File watching happens in background thread

## API Reference

### runWithAutoRestart()

```java
JavalinDevMode.runWithAutoRestart(() -> {
    // Create and return your Javalin instance
    return Javalin.create().start(7070);
});
```

This method:
- Blocks the main thread (keeps JVM alive)
- Automatically detects class directories
- Starts file watching in background
- Calls your factory function to create initial app
- Restarts app when changes detected

### shutdown()

```java
JavalinDevMode.shutdown();
```

Stops the development mode and shuts down the current server. Usually not needed as the JVM shutdown hook handles this automatically.

## Requirements

- Java 17+ (same as Javalin)
- Maven or Gradle project structure
- Build tool running in continuous compilation mode

## Limitations

### What JavalinDevMode Does

✅ Watches for `.class` file changes  
✅ Automatically restarts the Javalin server  
✅ Keeps the JVM alive between restarts  
✅ Works with any build tool  

### What JavalinDevMode Does NOT Do

❌ Recompile your source files (use your build tool for this)  
❌ Hot-swap classes without restart (use JRebel/DCEVM for this)  
❌ Reload classes in-place (requires JVM agents)  
❌ Preserve application state across restarts  

## Comparison with Other Solutions

| Solution | Setup | Class Reload | Works with All Build Tools |
|----------|-------|--------------|----------------------------|
| JavalinDevMode | Minimal | No (full restart) | Yes |
| JRebel | License + Agent | Yes | Yes |
| DCEVM + HotswapAgent | Agent | Yes | Yes |
| Spring DevTools | Framework-specific | No (full restart) | No |
| Quarkus Dev Mode | Framework-specific | Yes | No |

## Best Practices

### Development Workflow

1. Start your build tool's continuous compilation in one terminal
2. Start your app with JavalinDevMode in another terminal
3. Edit your code and save
4. Build tool automatically recompiles
5. JavalinDevMode automatically restarts
6. Refresh your browser to see changes

### Tips

- Use `config.bundledPlugins.enableDevLogging()` to see detailed request logs
- Keep your app initialization fast for quicker restarts
- Use separate terminal windows/tabs for build tool and app
- Consider using a process manager like tmux or screen for managing multiple terminals

## Troubleshooting

### "No class directories found"

Make sure you're running from your project root directory where `target/classes` or `build/classes` exists. Run `mvn compile` or `gradle classes` at least once to create these directories.

### Server doesn't restart

1. Check that your build tool is in continuous mode
2. Verify that `.class` files are being updated in `target/classes` or `build/classes`
3. Look for error messages in the console
4. Ensure at least 2 seconds pass between restarts (minimum interval)

### Port already in use

The old server might not have stopped cleanly. Wait a few seconds or use a different port.

## Example Projects

See [`JavalinDevModeExample.java`](../javalin/src/test/java/io/javalin/examples/JavalinDevModeExample.java) for a complete working example.

## Related

- [Javalin Documentation](https://javalin.io/documentation)
- [DevLoggingPlugin](https://javalin.io/plugins#devloggingplugin) - Detailed request/response logging for development
