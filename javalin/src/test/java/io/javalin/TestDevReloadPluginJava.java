/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.testing.TestUtil;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Java integration tests for the DevReloadPlugin.
 * These tests use only the public API — no access to internals.
 * They verify the end-to-end flow: start server, modify source, make request, see new content.
 */
class TestDevReloadPluginJava {

    private static final Path CONTROLLER_FILE = Path.of("src/test/java/devreloadtest/DevReloadController.java");

    @Test
    void source_change_is_picked_up_on_next_request() throws Exception {
        String originalContent = Files.readString(CONTROLLER_FILE);
        try {
            TestUtil.test(
                Javalin.create(config -> {
                    config.routes.get("/test", ctx -> devreloadtest.DevReloadController.handle(ctx));
                    config.bundledPlugins.enableDevReload();
                }),
                (app, http) -> {
                    assertThat(http.getBody("/test")).isEqualTo("original");

                    Files.writeString(CONTROLLER_FILE, originalContent.replace("\"original\"", "\"from-java\""));
                    Thread.sleep(50);

                    assertThat(http.getBody("/test")).isEqualTo("from-java");
                }
            );
        } finally {
            Files.writeString(CONTROLLER_FILE, originalContent);
        }
    }

    @Test
    void unchanged_files_do_not_cause_reload() {
        TestUtil.test(
            Javalin.create(config -> {
                config.routes.get("/stable", ctx -> ctx.result("steady"));
                config.bundledPlugins.enableDevReload();
            }),
            (app, http) -> {
                assertThat(http.getBody("/stable")).isEqualTo("steady");
                assertThat(http.getBody("/stable")).isEqualTo("steady");
                assertThat(http.getBody("/stable")).isEqualTo("steady");
            }
        );
    }

    @Test
    void server_stays_running_through_reload() throws Exception {
        String originalContent = Files.readString(CONTROLLER_FILE);
        try {
            TestUtil.test(
                Javalin.create(config -> {
                    config.routes.get("/test", ctx -> devreloadtest.DevReloadController.handle(ctx));
                    config.routes.get("/other", ctx -> ctx.result("other"));
                    config.bundledPlugins.enableDevReload();
                }),
                (app, http) -> {
                    int port = app.port();

                    Files.writeString(CONTROLLER_FILE, originalContent.replace("\"original\"", "\"reloaded\""));
                    Thread.sleep(50);

                    assertThat(http.getBody("/test")).isEqualTo("reloaded");
                    assertThat(app.port()).isEqualTo(port);
                    assertThat(app.jettyServer().server().isRunning()).isTrue();
                }
            );
        } finally {
            Files.writeString(CONTROLLER_FILE, originalContent);
        }
    }

    @Test
    void multiple_reloads_work() throws Exception {
        String originalContent = Files.readString(CONTROLLER_FILE);
        try {
            TestUtil.test(
                Javalin.create(config -> {
                    config.routes.get("/test", ctx -> devreloadtest.DevReloadController.handle(ctx));
                    config.bundledPlugins.enableDevReload();
                }),
                (app, http) -> {
                    assertThat(http.getBody("/test")).isEqualTo("original");

                    Files.writeString(CONTROLLER_FILE, originalContent.replace("\"original\"", "\"v2\""));
                    Thread.sleep(50);
                    assertThat(http.getBody("/test")).isEqualTo("v2");

                    Files.writeString(CONTROLLER_FILE, originalContent.replace("\"original\"", "\"v3\""));
                    Thread.sleep(50);
                    assertThat(http.getBody("/test")).isEqualTo("v3");
                }
            );
        } finally {
            Files.writeString(CONTROLLER_FILE, originalContent);
        }
    }
}
