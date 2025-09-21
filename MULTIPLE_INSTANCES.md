# Multiple Javalin Instances in the Same JVM

## Summary

**Yes, Javalin fully supports multiple instances in the same JVM.** Unlike Spark, Javalin is designed with instance-based architecture from the ground up, making it safe and straightforward to run multiple instances concurrently.

## Key Points

1. **Instance-Based Design**: Each Javalin instance has its own isolated configuration, server, and resources
2. **Thread Safety**: The static API (ApiBuilder) uses ThreadLocal storage for thread safety
3. **No Special Configuration Required**: Multiple instances work out of the box with `Javalin.createAndStart()`
4. **Separate Jetty Servers**: Each instance runs its own Jetty server on different ports
5. **Independent Lifecycles**: Instances can be started and stopped independently

## Usage Pattern (From Issue #2383)

Your current usage pattern is correct and fully supported:

```java
// Instance 1
Javalin javalin1 = Javalin.createAndStart((JavalinConfig config) -> {
    configure(config);  // Your custom configuration
});

// Instance 2  
Javalin javalin2 = Javalin.createAndStart((JavalinConfig config) -> {
    configure(config);  // Your custom configuration
});
```

## Architecture Details

### Isolation Guarantees

- **Server Isolation**: Each instance creates its own Jetty server
- **Configuration Isolation**: `JavalinConfig` is per-instance
- **Thread Pool Isolation**: Each instance can have its own thread pool
- **Route Isolation**: Routes are registered per-instance
- **Context Path Isolation**: Each instance can have different context paths

### Thread Safety

- **ApiBuilder**: Uses `ThreadLocal<JavalinDefaultRoutingApi<?>>` for thread-safe static routing
- **No Shared State**: No static variables that could cause interference
- **Concurrent Creation**: Instances can be created and started concurrently

## Troubleshooting Intermittent Issues

If you're experiencing instances that "stop responding", consider these potential causes:

### 1. Thread Pool Configuration
```java
Javalin.createAndStart(config -> {
    // Explicit thread pool configuration
    config.jetty.threadPool = ConcurrencyUtil.jettyThreadPool("MyApp", 10, 200, false);
});
```

### 2. Resource Limits
- Check JVM heap size and thread limits
- Monitor for thread starvation
- Consider using separate thread pools per instance

### 3. Port Conflicts
```java
// Ensure different ports
Javalin app1 = Javalin.createAndStart(config -> {
    config.jetty.defaultPort = 8080;
});
Javalin app2 = Javalin.createAndStart(config -> {
    config.jetty.defaultPort = 8081;
});
```

### 4. Graceful Shutdown
```java
// Proper cleanup
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    app1.stop();
    app2.stop();
}));
```

## Best Practices

1. **Use Different Ports**: Either specify different ports or use port 0 for auto-assignment
2. **Independent Configuration**: Don't share configuration objects between instances
3. **Proper Lifecycle Management**: Track instances and ensure proper shutdown
4. **Monitor Resources**: Watch thread usage and memory consumption
5. **Logging Configuration**: Use different loggers or log prefixes to distinguish instances

## Validation

The comprehensive test suite in this repository validates:
- Multiple instances on different ports
- Concurrent request handling across instances
- Configuration isolation
- Thread pool separation
- ApiBuilder thread safety
- Load testing with multiple instances

All tests pass, confirming robust support for multiple instances.

## Conclusion

Multiple Javalin instances in the same JVM are fully supported and should work reliably. The intermittent issues you're experiencing are likely related to resource configuration, monitoring, or external factors rather than fundamental limitations of Javalin's multi-instance support.