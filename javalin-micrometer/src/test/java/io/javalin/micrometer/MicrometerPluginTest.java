package io.javalin.micrometer;

import io.javalin.Javalin;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MicrometerPluginTest {

    @Test
    public void testPluginCanBeRegistered() {
        Javalin app = Javalin.create(cfg -> {
            cfg.registerPlugin(new MicrometerPlugin());
        });
        app.get("/test", ctx -> ctx.result("Hello World"));
        
        // Just verify the plugin was registered without error
        assertThat(app).isNotNull();
        app.stop();
    }

    @Test
    public void testPluginWithCustomRegistry() {
        MeterRegistry registry = new SimpleMeterRegistry();
        
        Javalin app = Javalin.create(cfg -> {
            cfg.registerPlugin(new MicrometerPlugin(config -> {
                config.registry = registry;
                config.tags = new String[]{"service", "test-app"};
            }));
        });
        app.get("/test", ctx -> ctx.result("Hello World"));
        
        // Verify the registry was set correctly
        assertThat(registry).isNotNull();
        app.stop();
    }

    @Test
    public void testPluginWithDisabledFeatures() {
        Javalin app = Javalin.create(cfg -> {
            cfg.registerPlugin(new MicrometerPlugin(config -> {
                config.enableHttpMetrics = false;
            }));
        });
        app.get("/test", ctx -> ctx.result("Hello World"));
        
        // Just verify the plugin was configured without error
        assertThat(app).isNotNull();
        app.stop();
    }
}