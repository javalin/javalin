# Javalin Micrometer Plugin [![javadoc](https://javadoc.io/badge2/io.javalin/javalin-micrometer/javadoc.svg)](https://javadoc.io/doc/io.javalin/javalin-micrometer)

Micrometer metrics integration for Javalin! This plugin provides automatic HTTP request metrics and supports various metrics registries.

## Getting started

Add the dependency to your project:

### Maven

```xml
<dependency>
  <groupId>io.javalin</groupId>
  <artifactId>javalin-micrometer</artifactId>
  <version>${javalin.version}</version>
</dependency>
```

### Gradle

```kotlin
implementation("io.javalin:javalin-micrometer:$javalinVersion")
```

## Module Support

This module provides full Java Module System (JPMS) support:

- **Module name**: `io.javalin.micrometer`
- **Automatic-Module-Name**: `io.javalin.micrometer` (for non-modular environments)

Add to your `module-info.java`:

```java
requires io.javalin.micrometer;
```

## Configuration

Register the plugin when creating your Javalin application:

### Java

```java
import io.javalin.micrometer.MicrometerPlugin;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

Javalin.create(config -> {
    config.registerPlugin(new MicrometerPlugin()); // Uses global registry
    // OR with custom configuration
    config.registerPlugin(new MicrometerPlugin(plugin -> {
        plugin.registry = new SimpleMeterRegistry();
        plugin.tags = Tags.of("service", "my-app", "version", "1.0");
        plugin.tagExceptionName = true;
    }));
});
```

### Kotlin

```kotlin
import io.javalin.micrometer.MicrometerPlugin
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

Javalin.create { config ->
    config.registerPlugin(MicrometerPlugin()) // Uses global registry
    // OR with custom configuration
    config.registerPlugin(MicrometerPlugin { plugin ->
        plugin.registry = SimpleMeterRegistry()
        plugin.tags = Tags.of("service", "my-app", "version", "1.0")
        plugin.tagExceptionName = true
    })
}
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `registry` | `MeterRegistry` | `Metrics.globalRegistry` | The meter registry to use for metrics |
| `tags` | `Iterable<Tag>` | `Tags.empty()` | Additional tags to add to all metrics |
| `tagExceptionName` | `boolean` | `false` | Whether to tag exception names in metrics |
| `tagRedirectPaths` | `boolean` | `false` | Whether to tag redirect paths with actual path instead of "REDIRECTION" |
| `tagNotFoundMappedPaths` | `boolean` | `false` | Whether to tag 404s from mapped paths with actual path instead of "NOT_FOUND" |

## Metrics Collected

### HTTP Request Metrics

- **Timer**: `http.server.requests` - Request duration with tags:
  - `method` - HTTP method (GET, POST, etc.)
  - `uri` - Request URI template (parameterized to avoid high cardinality)
  - `status` - HTTP status code
  - `outcome` - Request outcome (SUCCESS, CLIENT_ERROR, SERVER_ERROR, REDIRECTION)
  - `exception` - Exception name (if `tagExceptionName` is enabled and exception was thrown)
  - Any custom tags configured

### Jetty Server Metrics

- **Thread pool metrics** - Jetty server thread pool statistics
- **Connection metrics** - HTTP connection statistics

## Registry Support

The plugin works with any Micrometer registry:

```java
// Prometheus
PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

// InfluxDB
InfluxMeterRegistry influxRegistry = new InfluxMeterRegistry(InfluxConfig.DEFAULT);

// CloudWatch
CloudWatchMeterRegistry cloudWatchRegistry = new CloudWatchMeterRegistry(CloudWatchConfig.DEFAULT);

// Use any registry
config.registerPlugin(new MicrometerPlugin(plugin -> {
    plugin.registry = prometheusRegistry; // or influxRegistry, cloudWatchRegistry, etc.
}));
```

## Example with Prometheus

```java
import io.javalin.Javalin;
import io.javalin.micrometer.MicrometerPlugin;
import io.micrometer.core.instrument.Tags;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

public class App {
    public static void main(String[] args) {
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        
        Javalin app = Javalin.create(config -> {
            config.registerPlugin(new MicrometerPlugin(plugin -> {
                plugin.registry = prometheusRegistry;
                plugin.tags = Tags.of("service", "my-api");
                plugin.tagExceptionName = true;
            }));
        });
        
        app.get("/", ctx -> ctx.result("Hello World"));
        app.get("/metrics", ctx -> ctx.result(prometheusRegistry.scrape()));
        
        // Delegate exceptions to Micrometer for proper tagging
        app.exception(Exception.class, MicrometerPlugin.exceptionHandler);
        
        app.start(7000);
    }
}
```

## Exception Tagging

To properly tag exceptions in metrics, you need to delegate to the Micrometer exception handler:

```java
app.exception(Exception.class, MicrometerPlugin.exceptionHandler);
// Or for specific exceptions:
app.exception(IllegalArgumentException.class, (e, ctx) -> {
    MicrometerPlugin.exceptionHandler.handle(e, ctx);
});
```

## Requirements

- Java 17+
- Javalin 7.0+
- Jetty 12+
- Micrometer Core 1.14+

## About Javalin [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![Chat at https://discord.gg/sgak4e5NKv](https://img.shields.io/badge/chat-on%20Discord-%234cb697)](https://discord.gg/sgak4e5NKv)

* [:heart: Sponsor Javalin](https://github.com/sponsors/tipsy)
* The main project webpage is [javalin.io](https://javalin.io)
* Chat on Discord: <https://discord.gg/sgak4e5NKv>
* License summary: <https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)>
