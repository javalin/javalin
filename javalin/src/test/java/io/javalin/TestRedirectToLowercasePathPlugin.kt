/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.HttpCodes.IM_A_TEAPOT
import io.javalin.http.HttpCodes.MOVED_PERMANENTLY
import io.javalin.plugin.RedirectToLowercasePathPlugin
import io.javalin.testing.TestUtil
import kong.unirest.HttpResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class TestRedirectToLowercasePathPlugin {

    private val testApp by lazy {
        Javalin.create {
            it.plugins.register(RedirectToLowercasePathPlugin())
        }
    }

    @Test
    fun `exception is thrown when using non-lowercase paths with TestRedirectToLowercasePathPlugin`() = TestUtil.test(testApp) { app, _ ->
        assertThatIllegalArgumentException()
            .isThrownBy { app.get("/TEST") { } }
            .withMessage("Paths must be lowercase when using RedirectToLowercasePathPlugin")

        // complex segments are handled differently internally
        assertThatIllegalArgumentException()
            .isThrownBy { app.get("/HI-{world}") { } }
            .withMessage("Paths must be lowercase when using RedirectToLowercasePathPlugin")
    }

    @Test
    fun `exception is NOT thrown when using uppercase path params with TestRedirectToLowercasePathPlugin`() = TestUtil.test(testApp) { app, _ ->
        app.get("/{TEST}") { }
    }

    @Test
    fun `only wrong cased requests are redirected`() = TestUtil.test(testApp) { app, http ->
        app.get("/my-endpoint") { it.status(IM_A_TEAPOT) }
        http.disableUnirestRedirects()
        assertThat(http.get("/my-endpoint").status).isEqualTo(IM_A_TEAPOT.status)
        assertThat(http.get("/my-eNdPOinT").status).isEqualTo(MOVED_PERMANENTLY.status)
        http.enableUnirestRedirects()
        assertThat(http.get("/my-eNdPOinT").status).isEqualTo(IM_A_TEAPOT.status)
    }

    @Test
    fun `only wrong cased requests are redirected complex segments edition`() = TestUtil.test(testApp) { app, http ->
        app.get("/my-{endpoint}") { it.status(IM_A_TEAPOT).result(it.pathParam("endpoint")) }
        http.disableUnirestRedirects()
        http.get("/my-endpoint").assertStatusAndBodyMatch(IM_A_TEAPOT.status, "endpoint")
        http.get("/my-ENDPOINT").assertStatusAndBodyMatch(IM_A_TEAPOT.status, "ENDPOINT")
        http.get("/MY-eNdPOinT").assertStatusAndBodyMatch(MOVED_PERMANENTLY.status, "Redirected")
        http.enableUnirestRedirects()
        http.get("/MY-eNdPOinT").assertStatusAndBodyMatch(IM_A_TEAPOT.status, "eNdPOinT")
    }

    @Test
    fun `non-lowercase path-params are not redirected`() = TestUtil.test(testApp) { app, http ->
        app.get("/path/{param}") { it.status(IM_A_TEAPOT) }
        http.disableUnirestRedirects()
        assertThat(http.get("/path/OnE").status).isEqualTo(IM_A_TEAPOT.status)
        http.enableUnirestRedirects()
    }

    @Test
    fun `query params are kept after redirect`() = TestUtil.test(testApp) { app, http ->
        app.get("/lowercase") { it.result(it.queryParam("qp")!!) }
        assertThat(http.getBody("/LOWERCASE?qp=UPPERCASE")).isEqualTo("UPPERCASE")
    }

    @Test
    fun `path params and query params work as expected`() = TestUtil.test(testApp) { app, http ->
        app.get("/user/{userId}") { it.result(it.pathParam("userId") + " " + it.queryParam("qp")!!) }
        assertThat(http.getBody("/UsEr/pkkummermo?qp=GladGutt")).isEqualTo("pkkummermo GladGutt")
    }

    @Test
    fun `path params follow by splat works`() = TestUtil.test(testApp) { app, http ->
        app.get("/{param}/*") { it.result(it.path()) }
        assertThat(http.getBody("/PaRaM/sPlAt")).isEqualTo("/PaRaM/sPlAt")
    }

    private fun HttpResponse<String?>.assertStatusAndBodyMatch(status: Int, body: String) {
        assertThat(this.status).isEqualTo(status)
        assertThat(this.body).isNotNull.isEqualTo(body)
    }

}
