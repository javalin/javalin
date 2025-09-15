package io.javalin.micrometer;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.plugin.Plugin;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Plugin that integrates Micrometer metrics with Javalin.
 * Provides HTTP request metrics and basic application metrics.
 */
public class MicrometerPlugin extends Plugin<MicrometerPlugin.Config> {
    
    private static final Logger logger = LoggerFactory.getLogger(MicrometerPlugin.class);

    public MicrometerPlugin() {
        this(null);
    }

    public MicrometerPlugin(Consumer<Config> userConfig) {
        super(userConfig, new Config());
    }

    public static class Config {
        /** The meter registry to use. Defaults to the global registry. */
        public MeterRegistry registry = Metrics.globalRegistry;
        
        /** Whether to enable HTTP request metrics */
        public boolean enableHttpMetrics = true;
        
        /** Tags to add to all metrics */
        public String[] tags = new String[0];
        
        /** Metric name for HTTP requests */
        public String httpRequestsMetricName = "http.server.requests";
    }

    @Override
    public void onInitialize(JavalinConfig config) {
        if (pluginConfig.enableHttpMetrics) {
            setupHttpMetrics(config);
        }
        logger.info("MicrometerPlugin initialized with registry: {}", pluginConfig.registry.getClass().getSimpleName());
    }

    private void setupHttpMetrics(JavalinConfig config) {
        config.requestLogger.http(this::recordHttpRequest);
    }

    private void recordHttpRequest(Context ctx, Float executionTimeMs) {
        try {
            String method = ctx.method().toString();
            String uri = getUriTemplate(ctx);
            String status = String.valueOf(ctx.status().getCode());
            
            // Record request duration
            Timer.builder(pluginConfig.httpRequestsMetricName)
                .description("HTTP requests")
                .tag("method", method)
                .tag("uri", uri)
                .tag("status", status)
                .tags(convertTags(pluginConfig.tags))
                .register(pluginConfig.registry)
                .record(executionTimeMs != null ? (long) (float) executionTimeMs : 0, java.util.concurrent.TimeUnit.MILLISECONDS);
                
            // Record request count
            Counter.builder(pluginConfig.httpRequestsMetricName + ".total")
                .description("Total HTTP requests")
                .tag("method", method)
                .tag("uri", uri)
                .tag("status", status)
                .tags(convertTags(pluginConfig.tags))
                .register(pluginConfig.registry)
                .increment();
                
        } catch (Exception e) {
            logger.warn("Failed to record HTTP metrics", e);
        }
    }

    private String getUriTemplate(Context ctx) {
        try {
            // Try to get the matched path (route template)
            String matchedPath = ctx.matchedPath();
            if (matchedPath != null && !matchedPath.isEmpty()) {
                return matchedPath;
            }
            // Fallback to the actual path, but avoid high cardinality
            String path = ctx.path();
            if (path.matches(".*\\d+.*")) {
                return "DYNAMIC_PATH";
            }
            return path;
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    private String[] convertTags(String[] tags) {
        if (tags == null || tags.length == 0) {
            return new String[0];
        }
        return tags;
    }
}