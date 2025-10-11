# DevReloadPlugin

The `DevReloadPlugin` enables automatic file change detection during development. It watches specified directories and triggers callbacks when changes are detected.

## Important Notes

⚠️ **This plugin provides FILE WATCHING ONLY** - it detects changes but does NOT automatically reload classes.

For actual hot reloading, you need to integrate with your build tool (Maven/Gradle/Bazel) or use a dedicated hot reload solution.

## Features

- Cross-platform file watching using Java NIO WatchService
- Configurable watch paths (directories to monitor)
- Debounce delay to avoid rapid repeated triggers
- Custom reload callback support
- Verbose logging option
- Daemon thread for background watching
- Automatic cleanup on server stop

## Basic Usage

```java
Javalin.create(config -> {
    config.bundledPlugins.enableDevReload(plugin -> {
        plugin.watchPaths = List.of(
            "src/main/java",
            "src/main/resources"
        );
        plugin.debounceDelayMs = 500;
        plugin.verbose = true;
        plugin.onReload = app -> {
            System.out.println("Changes detected!");
            // Implement your reload logic here
        };
    });
}).start(7070);
```

## Recommended Workflow

### Option A: Watch Compiled Output (Recommended)

Watch your build output directory and use continuous compilation:

```bash
# Terminal 1: Continuous compilation
mvn compile -DskipTests --watch

# Terminal 2: Run your app
```

```java
config.bundledPlugins.enableDevReload(plugin -> {
    plugin.watchPaths = List.of("target/classes");  // Watch compiled output
    plugin.onReload = app -> {
        // Restart logic here
    };
});
```

### Option B: Watch Source Files

Watch source directories for any changes (won't reload classes automatically):

```java
config.bundledPlugins.enableDevReload(plugin -> {
    plugin.watchPaths = List.of(
        "src/main/java",
        "src/main/kotlin",
        "src/main/resources"
    );
    plugin.onReload = app -> {
        // Trigger your build tool or notify developer
        System.out.println("Source files changed!");
    };
});
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `watchPaths` | `List<String>` | `[]` | Directories to watch for changes (relative to project root) |
| `debounceDelayMs` | `long` | `500` | Wait time (ms) after last change before triggering callback |
| `onReload` | `Consumer<Javalin>` | `null` | Custom callback when changes detected |
| `verbose` | `boolean` | `true` | Enable detailed logging of file changes |

## Integration with Build Tools

### Maven

Use with Maven's continuous build:

```bash
# Continuous compilation
mvn compile -DskipTests --watch

# Or use exec plugin with auto-restart
mvn exec:java -Dexec.mainClass="com.example.App"
```

### Gradle

Use with Gradle's continuous build:

```bash
gradle build --continuous
```

### Build-Tool Agnostic

This plugin works without any specific build tool integration, making it useful for:
- Projects using Bazel or other build systems
- Custom build setups
- Notification-only scenarios

## Alternatives for Full Hot Reload

For production-ready hot reload with automatic class reloading:

1. **JRebel** (Commercial) - Most comprehensive solution
2. **DCEVM + HotswapAgent** (Open Source) - Free alternative
3. **Spring DevTools** (If using Spring) - Automatic restart
4. **Quarkus Dev Mode** (If using Quarkus) - Built-in hot reload
5. **Build Tool Plugins** - Maven/Gradle plugins with process restart

## Example

See [DevReloadExample.java](../javalin/src/test/java/io/javalin/examples/DevReloadExample.java) for a complete example.

## Limitations

- Only watches for file system events (create, modify, delete)
- Does not reload Java classes automatically
- Does not restart the server automatically (requires custom implementation)
- Watches directories only (not recursive by default)
- Performance depends on number of files being watched

## Related

- [DevLoggingPlugin](./dev-logging-plugin.md) - Development request/response logger
- [Quarkus Dev Mode](https://quarkus.io/guides/getting-started#development-mode) - Similar approach in Quarkus
