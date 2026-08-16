/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.plugin.bundled.DevReloadPlugin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Java API tests for DevReloadPlugin — verify the public Java API works.
 */
@SuppressWarnings("deprecation")
class TestDevReloadPluginJava {

    @Test
    void zero_config_registration_from_java() {
        JavalinTest.test(
            Javalin.create(config -> {
                config.routes.get("/hello", ctx -> ctx.result("Hello from Java"));
                config.registerPlugin(new DevReloadPlugin(c -> c.logging = DevReloadPlugin.LogLevel.NONE));
            }),
            (app, client) -> {
                var response = client.get("/hello");
                assertThat(response.code()).isIn(200, 503);
            }
        );
    }

    @Test
    void custom_config_from_java() {
        JavalinTest.test(
            Javalin.create(config -> {
                config.routes.get("/test", ctx -> ctx.result("Test"));
                config.registerPlugin(new DevReloadPlugin(c -> {
                    c.compileCommand = "echo compiled";
                    c.mainClass = "com.example.Main";
                    c.watchCooldownMs = 100;
                    c.logging = DevReloadPlugin.LogLevel.NONE;
                }));
            }),
            (app, client) -> {
                var response = client.get("/test");
                assertThat(response.code()).isIn(200, 503);
            }
        );
    }

    @Test
    void child_mode_app_works_normally() {
        System.setProperty("javalin.devreload.child", "true");
        try {
            JavalinTest.test(
                Javalin.create(config -> {
                    config.routes.get("/hello", ctx -> ctx.result("child-hello"));
                    config.registerPlugin(new DevReloadPlugin(c -> c.logging = DevReloadPlugin.LogLevel.NONE));
                }),
                (app, client) -> {
                    assertThat(client.get("/hello").body().string()).isEqualTo("child-hello");
                }
            );
        } finally {
            System.clearProperty("javalin.devreload.child");
        }
    }
}
