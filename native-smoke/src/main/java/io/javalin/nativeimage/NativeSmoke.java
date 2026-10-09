package io.javalin.nativeimage;

import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.util.ConcurrencyUtil;
import io.javalin.vue.JavalinVueConfig;
import java.time.LocalDate;
import java.util.Map;

/** Run on a JVM or as a native executable, then exercise it with check.sh. */
public class NativeSmoke {
    public static void main(String[] args) {
        if ("runtime".equals(System.getProperty("org.graalvm.nativeimage.imagecode"))) {
            try {
                new JavalinVueConfig().rootDirectory("/vue");
                throw new AssertionError("Classpath Vue scanning must fail with an actionable message");
            } catch (IllegalStateException expected) {
                if (!expected.getMessage().contains("rootDirectory(Path)")) throw expected;
            }
        }
        var mapper = new JavalinJackson();
        // JavaTimeModule is registered by class name, not referenced by this app.
        LocalDate date = mapper.fromJsonString("[2026,10,9]", LocalDate.class);
        if (!date.equals(LocalDate.of(2026, 10, 9))) throw new AssertionError(date);
        Javalin.create(config -> {
            config.concurrency.useVirtualThreads = true;
            config.jsonMapper(mapper);
            config.routes.get("/health", ctx -> ctx.result("native-ok"));
            config.routes.get("/json", ctx -> ctx.json(Map.of("status", "ok")));
            config.routes.post("/echo", ctx -> ctx.json(ctx.bodyAsClass(Map.class)));
            config.routes.get("/async", ctx -> ctx.async(() -> ctx.result("async-ok")));
            config.routes.get("/loom", ctx -> ctx.result(String.valueOf(ConcurrencyUtil.isLoomAvailable())));
        }).start(7070);
    }
}
