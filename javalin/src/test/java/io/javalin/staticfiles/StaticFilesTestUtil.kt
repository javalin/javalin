package io.javalin.staticfiles

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.http.staticfiles.JavalinStaticResourceHandler
import io.javalin.jetty.JettyResourceHandler
import io.javalin.testing.HttpUtil
import io.javalin.testing.TestUtil
import io.javalin.testing.ThrowingBiConsumer
import java.util.function.Consumer

/**
 * Runs the test twice: once with JettyResourceHandler and once with JavalinStaticResourceHandler.
 *
 * @param configurer a consumer that configures the Javalin instance (typically adding static files)
 * @param userCode the test code to run
 */
fun testStaticFiles(configurer: Consumer<JavalinConfig>, userCode: ThrowingBiConsumer<Javalin, HttpUtil>) {
    // Test with JettyResourceHandler (default)
    TestUtil.test(Javalin.create { cfg ->
        cfg.resourceHandler(JettyResourceHandler())
        configurer.accept(cfg)
    }, userCode)

    // Test with JavalinStaticResourceHandler
    TestUtil.test(Javalin.create { cfg ->
        cfg.resourceHandler(JavalinStaticResourceHandler())
        configurer.accept(cfg)
    }, userCode)
}

/**
 * Runs the test twice with no static file configuration: once with JettyResourceHandler and once with JavalinStaticResourceHandler.
 *
 * @param userCode the test code to run
 */
fun testStaticFiles(userCode: ThrowingBiConsumer<Javalin, HttpUtil>) {
    testStaticFiles({ }, userCode)
}

