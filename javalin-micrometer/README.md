# About Javalin [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![Chat at https://discord.gg/sgak4e5NKv](https://img.shields.io/badge/chat-on%20Discord-%234cb697)](https://discord.gg/sgak4e5NKv)

* [:heart: Sponsor Javalin](https://github.com/sponsors/tipsy)
* The main project webpage is [javalin.io](https://javalin.io)
* Chat on Discord: <https://discord.gg/sgak4e5NKv>
* License summary: <https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)>

# Micrometer Plugin [![javadoc](https://javadoc.io/badge2/io.javalin/javalin-micrometer/javadoc.svg)](https://javadoc.io/doc/io.javalin/javalin-micrometer)

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
        plugin.tags = new String[]{"service", "my-app", "version", "1.0"};
        plugin.enableHttpMetrics = true;
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
        plugin.tags = arrayOf("service", "my-app", "version", "1.0")
        plugin.enableHttpMetrics = true
    })
}
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `registry` | `MeterRegistry` | `Metrics.globalRegistry` | The meter registry to use for metrics |
| `enableHttpMetrics` | `boolean` | `true` | Enable HTTP request metrics collection |
| `tags` | `String[]` | `[]` | Additional tags to add to all metrics |
| `httpRequestsMetricName` | `String` | `"http.server.requests"` | Name for HTTP request metrics |

## Metrics Collected

When `enableHttpMetrics` is enabled, the plugin automatically collects:

### HTTP Request Metrics

- **Timer**: `http.server.requests` - Request duration with tags:
  - `method` - HTTP method (GET, POST, etc.)
  - `uri` - Request URI template (parameterized to avoid high cardinality)
  - `status` - HTTP status code
  - Any custom tags configured

- **Counter**: `http.server.requests.total` - Total request count with the same tags

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
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

public class App {
    public static void main(String[] args) {
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        
        Javalin app = Javalin.create(config -> {
            config.registerPlugin(new MicrometerPlugin(plugin -> {
                plugin.registry = prometheusRegistry;
                plugin.tags = new String[]{"service", "my-api"};
            }));
        });
        
        app.get("/", ctx -> ctx.result("Hello World"));
        app.get("/metrics", ctx -> ctx.result(prometheusRegistry.scrape()));
        
        app.start(7000);
    }
}
```

## Requirements

- Java 17+
- Javalin 7.0+
- Micrometer Core 1.14+