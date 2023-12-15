package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.TooManyRequestsResponse;
import io.javalin.plugin.ContextPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class HelloWorldPlugin {
    public static void main(String[] args) {
        var app = Javalin.create(config -> {
            config.registerPlugin(new JRate(it -> it.limit = 1)); // register plugin with config
        });
        app.get("/", ctx -> {
            ctx.with(JRate.class).tryConsume(2);
            ctx.result("Hello World");
        });
        app.start(7070);
    }
}

// this class demonstrates the most advanced use case of a plugin,
// where the plugin has a config and a plugin extension
// we recommend using inner classes for plugins, as it keeps the whole plugin in one place
class JRate extends ContextPlugin<JRate.Config, JRate.Extension> {
    public JRate(Consumer<Config> userConfig) {
        super(userConfig, new Config());
    }
    Map<String, Integer> ipToCounter = new HashMap<>();
    @Override
    public Extension createExtension(@NotNull Context context) {
        return new Extension(context);
    }
    public static class Config {
        public int limit = 1;
    }
    public class Extension {
        private final Context context;
        public Extension(Context context) {
            this.context = context;
        }
        public void tryConsume(int cost) {
            String ip = context.ip();
            int counter = ipToCounter.compute(ip, (k, v) -> v == null ? cost : v + cost);
            if (counter > pluginConfig.limit) {
                throw new TooManyRequestsResponse();
            }
        }
    }
}
