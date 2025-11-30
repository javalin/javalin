/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.HandlerType
import io.javalin.http.servlet.DefaultTasks
import io.javalin.router.Endpoint
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestUnsafeStateAccess {

    @Test
    fun `unsafe provides access to internal-only eventManager`() = TestUtil.test { app, _ ->
        assertThat(app.unsafe.eventManager).isNotNull
    }

    @Test
    fun `unsafe can access and manipulate appDataManager directly`() = TestUtil.test { app, _ ->
        val customKey = io.javalin.config.Key<String>("custom-internal-key")
        app.unsafe.appDataManager.register(customKey, "internal-value")
        assertThat(app.unsafe.appDataManager.get(customKey)).isEqualTo("internal-value")
    }

    @Test
    fun `unsafe can access and modify internal servlet request lifecycle`() = TestUtil.test { app, _ ->
        val lifecycle = app.unsafe.servletRequestLifecycle
        val originalSize = lifecycle.size
        lifecycle.add(DefaultTasks.ERROR)
        assertThat(lifecycle.size).isEqualTo(originalSize + 1)
    }

    @Test
    fun `unsafe can access internal routers not exposed in public API`() = TestUtil.test(Javalin.create()) { app, http ->
        app.unsafe.internalRouter.addHttpEndpoint(Endpoint.create(HandlerType.GET, "/").handler { it.result("Unsafe") })
        assertThat(http.get("/").body).isEqualTo("Unsafe")
    }

    @Test
    fun `unsafe can access lazily initialized servlet`() {
        val app = Javalin.create()
        assertThat(app.unsafe.servlet.isInitialized()).isFalse()
        app.unsafe.servlet.value // access value to initialize
        assertThat(app.unsafe.servlet.isInitialized()).isTrue()
    }

}
