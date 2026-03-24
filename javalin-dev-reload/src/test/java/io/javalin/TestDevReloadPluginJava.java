/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.plugin.bundled.DevReloadPlugin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Java E2E tests for DevReloadPlugin — public API only.
 * Verify: start server → modify source → make request → see new content.
 */
@SuppressWarnings("deprecation")
class TestDevReloadPluginJava {

    private static final Path CONTROLLER_FILE = Path.of("src/test/java/devreloadtest/DevReloadController.java");

    /** Runs a test with the controller file modified, restoring it afterward. */
    private void withModifiedController(String replacement, ThrowingRunnable test) throws Exception {
        String original = Files.readString(CONTROLLER_FILE);
        try {
            test.run();
        } finally {
            Files.writeString(CONTROLLER_FILE, original);
        }
    }

    @FunctionalInterface
    interface ThrowingRunnable { void run() throws Exception; }

    @Test
    void source_change_is_picked_up_on_next_request() throws Exception {
        String originalContent = Files.readString(CONTROLLER_FILE);
        withModifiedController("from-java", () ->
            JavalinTest.test(
                Javalin.create(config -> {
                    config.routes.get("/test", ctx -> devreloadtest.DevReloadController.handle(ctx));
                    config.registerPlugin(new DevReloadPlugin(reload -> reload.watchCooldownMs = 0));
                }),
                (app, client) -> {
                    assertThat(client.get("/test").body().string()).isEqualTo("original");
                    Files.writeString(CONTROLLER_FILE, originalContent.replace("\"original\"", "\"from-java\""));
                    Thread.sleep(50);
                    assertThat(client.get("/test").body().string()).isEqualTo("from-java");
                }
            )
        );
    }

    @Test
    void unchanged_files_do_not_cause_reload() {
        JavalinTest.test(
            Javalin.create(config -> {
                config.routes.get("/stable", ctx -> ctx.result("steady"));
                config.registerPlugin(new DevReloadPlugin());
            }),
            (app, client) -> {
                assertThat(client.get("/stable").body().string()).isEqualTo("steady");
                assertThat(client.get("/stable").body().string()).isEqualTo("steady");
                assertThat(client.get("/stable").body().string()).isEqualTo("steady");
            }
        );
    }

    @Test
    void server_stays_running_through_reload() throws Exception {
        String originalContent = Files.readString(CONTROLLER_FILE);
        withModifiedController("reloaded", () ->
            JavalinTest.test(
                Javalin.create(config -> {
                    config.routes.get("/test", ctx -> devreloadtest.DevReloadController.handle(ctx));
                    config.routes.get("/other", ctx -> ctx.result("other"));
                    config.registerPlugin(new DevReloadPlugin(reload -> reload.watchCooldownMs = 0));
                }),
                (app, client) -> {
                    int port = app.port();
                    Files.writeString(CONTROLLER_FILE, originalContent.replace("\"original\"", "\"reloaded\""));
                    Thread.sleep(50);
                    assertThat(client.get("/test").body().string()).isEqualTo("reloaded");
                    assertThat(app.port()).isEqualTo(port);
                    assertThat(app.jettyServer().server().isRunning()).isTrue();
                }
            )
        );
    }

    @Test
    void multiple_reloads_work() throws Exception {
        String originalContent = Files.readString(CONTROLLER_FILE);
        withModifiedController("v3", () ->
            JavalinTest.test(
                Javalin.create(config -> {
                    config.routes.get("/test", ctx -> devreloadtest.DevReloadController.handle(ctx));
                    config.registerPlugin(new DevReloadPlugin(reload -> reload.watchCooldownMs = 0));
                }),
                (app, client) -> {
                    assertThat(client.get("/test").body().string()).isEqualTo("original");
                    Files.writeString(CONTROLLER_FILE, originalContent.replace("\"original\"", "\"v2\""));
                    Thread.sleep(50);
                    assertThat(client.get("/test").body().string()).isEqualTo("v2");
                    Files.writeString(CONTROLLER_FILE, originalContent.replace("\"original\"", "\"v3\""));
                    Thread.sleep(50);
                    assertThat(client.get("/test").body().string()).isEqualTo("v3");
                }
            )
        );
    }
}
