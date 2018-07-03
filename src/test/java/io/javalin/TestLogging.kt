/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.util.HttpUtil
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TestLogging {

    @Test
    fun `default logging (no logging) works`() = runTest(Javalin.create())

    @Test
    fun `debug logging works`() = runTest(Javalin.create().enableDebugLogging())

    @Test
    fun `custom logging works`() = runTest(Javalin.create().requestLogger { ctx, executionTimeMs -> println("That took $executionTimeMs milliseconds") })

    private fun runTest(app: Javalin) {
        app.get("/blocking") { ctx -> ctx.result("Hello Blocking World!") }
        app.get("/async") { ctx ->
            val future = CompletableFuture<String>()
            Executors.newSingleThreadScheduledExecutor().schedule<Boolean>({ future.complete("Hello Async World!") }, 10, TimeUnit.MILLISECONDS)
            ctx.result(future)
        }
        app.start(0)
        val http = HttpUtil(app)
        assertThat(http.getBody("/async"), `is`("Hello Async World!"))
        assertThat(http.getBody("/blocking"), `is`("Hello Blocking World!"))
        app.stop()
    }

}
