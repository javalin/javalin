package io.javalin.examples.plugin;

import io.javalin.Javalin;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.javalin.http.Context;
import io.javalin.http.TooManyRequestsResponse;
import io.javalin.plugin.ContextPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class HelloWorldPluginJava {
    public static void main(String[] args) {
        // Create a Javalin app
        Javalin app = Javalin.create(config -> {
            config.registerPlugin(new Bucket4J(bucket4JConfig -> {
                bucket4JConfig.limit = Bandwidth.simple(10, Duration.ofMinutes(1));
            }));
        }).start(7000);

        app.get("/cheap-endpoint", ctx -> {
            ctx.with(Bucket4J.class).tryConsume(1);
            ctx.result("Hello, you've accessed the cheap endpoint!");
        });

        app.get("/expensive-endpoint", ctx -> {
            ctx.with(Bucket4J.class).tryConsume(5);
            ctx.result("Hello, you've accessed the expensive endpoint!");
        });
    }
}

class Bucket4J extends ContextPlugin<Bucket4J.Config, Bucket4J.Extension> {
    private Map<String, Bucket> buckets = new HashMap<>();

    public Bucket4J(Consumer<Config> userConfig) {
        super(userConfig, new Config());
    }

    @Override
    public Extension createExtension(@NotNull Context context) {
        return new Extension(context);
    }

    static class Config {
        public Bandwidth limit = Bandwidth.simple(10, Duration.ofMinutes(1));
    }

    public class Extension {
        Context ctx;

        public Extension(Context ctx) {
            this.ctx = ctx;
        }

        void tryConsume(int tokens){

            String ip = ctx.ip();
            Bucket bucket = buckets.computeIfAbsent(ip, k -> {
                return Bucket4j.builder().addLimit(getPluginConfig().limit).build();
            });
            boolean consumed = bucket.tryConsume(tokens);
            if (!consumed) {
                throw new TooManyRequestsResponse();
            }
        }
    }
}
