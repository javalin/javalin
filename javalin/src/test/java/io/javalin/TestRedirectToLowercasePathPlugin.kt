/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.util.RedirectToLowercasePathPlugin
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.Test

class TestRedirectToLowercasePathPlugin {

    private val testApp by lazy {
        Javalin.create {
            it.registerPlugin(RedirectToLowercasePathPlugin())
        }
    }

    @Test
    fun `exception is thrown when using non-lowercase paths with TestRedirectToLowercasePathPlugin`() = TestUtil.test(testApp) { app, http ->
        assertThatIllegalArgumentException()
                .isThrownBy { app.get("/TEST") { } }
                .withMessage("Paths must be lowercase when using RedirectToLowercasePathPlugin")
    }

    @Test
    fun `exception is NOT thrown when using uppercase path params with TestRedirectToLowercasePathPlugin`() = TestUtil.test(testApp) { app, http ->
        app.get("/:TEST") { }
    }

    @Test
    fun `only wrong cased requests are redirected`() = TestUtil.test(testApp) { app, http ->
        app.get("/my-endpoint") { it.status(418) }
        http.disableUnirestRedirects()
        assertThat(http.get("/my-endpoint").status).isEqualTo(418)
        assertThat(http.get("/my-eNdPOinT").status).isEqualTo(301)
        http.enableUnirestRedirects()
        assertThat(http.get("/my-eNdPOinT").status).isEqualTo(418)
    }

    @Test
    fun `non-lowercase path-params are not redirected`() = TestUtil.test(testApp) { app, http ->
        app.get("/path/:param") { it.status(418) }
        http.disableUnirestRedirects()
        assertThat(http.get("/path/OnE").status).isEqualTo(418)
        http.enableUnirestRedirects()
    }

    @Test
    fun `query params are kept after redirect`() = TestUtil.test(testApp) { app, http ->
        app.get("/lowercase") { it.result(it.queryParam("qp")!!) }
        assertThat(http.getBody("/LOWERCASE?qp=UPPERCASE")).isEqualTo("UPPERCASE")
    }

    @Test
    fun `path params and query params work as expected`() = TestUtil.test(testApp) { app, http ->
        app.get("/user/:userId") { it.result(it.pathParam("userId") + " " + it.queryParam("qp")!!) }
        assertThat(http.getBody("/UsEr/pkkummermo?qp=GladGutt")).isEqualTo("pkkummermo GladGutt")
    }

    @Test
    fun `path params follow by splat works`() = TestUtil.test(testApp) { app, http ->
        app.get("/:param/*") { it.result(it.path()) }
        assertThat(http.getBody("/PaRaM/sPlAt")).isEqualTo("/PaRaM/sPlAt")
    }

}
