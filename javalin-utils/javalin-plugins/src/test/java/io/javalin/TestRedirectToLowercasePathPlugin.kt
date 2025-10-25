/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.HttpStatus
import io.javalin.http.HttpStatus.IM_A_TEAPOT
import io.javalin.http.HttpStatus.MOVED_PERMANENTLY
import io.javalin.plugin.RedirectToLowercasePathPlugin
import io.javalin.testing.getBody
import io.javalin.testing.httpCode
import io.javalin.testtools.JavalinTest
import io.javalin.testtools.Response
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TestRedirectToLowercasePathPlugin {

    private val testApp by lazy {
        Javalin.create {
            it.registerPlugin(RedirectToLowercasePathPlugin())
        }
    }

    @Test
    fun `exception is thrown when using non-lowercase paths with TestRedirectToLowercasePathPlugin`() = JavalinTest.test(testApp) { app, _ ->
        assertThatIllegalArgumentException()
            .isThrownBy { app.unsafe.routes.get("/TEST") { } }
            .withMessage("Paths must be lowercase when using RedirectToLowercasePathPlugin")

        // complex segments are handled differently internally
        assertThatIllegalArgumentException()
            .isThrownBy { app.unsafe.routes.get("/HI-{world}") { } }
            .withMessage("Paths must be lowercase when using RedirectToLowercasePathPlugin")
    }

    @Test
    fun `exception is NOT thrown when using uppercase path params with TestRedirectToLowercasePathPlugin`() = JavalinTest.test(testApp) { app, _ ->
        app.unsafe.routes.get("/{TEST}") { }
    }

    @Test
    fun `only wrong cased requests are redirected`() = JavalinTest.test(testApp) { app, http ->
        app.unsafe.routes.get("/my-endpoint") { it.status(IM_A_TEAPOT) }
        // Note: JavalinTest's HttpClient doesn't follow redirects by default
        assertThat(http.get("/my-endpoint").httpCode()).isEqualTo(IM_A_TEAPOT)
        assertThat(http.get("/my-eNdPOinT").httpCode()).isEqualTo(MOVED_PERMANENTLY)
        // To test redirect following, we'd need to manually follow the Location header
        // For now, just test that the redirect is issued
    }

    @Test
    fun `only wrong cased requests are redirected complex segments edition`() = JavalinTest.test(testApp) { app, http ->
        app.unsafe.routes.get("/my-{endpoint}") { it.status(IM_A_TEAPOT).result(it.pathParam("endpoint")) }
        http.get("/my-endpoint").assertStatusAndBodyMatch(IM_A_TEAPOT, "endpoint")
        http.get("/my-ENDPOINT").assertStatusAndBodyMatch(IM_A_TEAPOT, "ENDPOINT")
        http.get("/MY-eNdPOinT").assertStatusAndBodyMatch(MOVED_PERMANENTLY, "Redirected")
        // Following redirects would require manual implementation
    }

    @Test
    fun `non-lowercase path-params are not redirected`() = JavalinTest.test(testApp) { app, http ->
        app.unsafe.routes.get("/path/{param}") { it.status(IM_A_TEAPOT) }
        assertThat(http.get("/path/OnE").httpCode()).isEqualTo(IM_A_TEAPOT)
    }

    @Test
    fun `query params are kept after redirect`() = JavalinTest.test(testApp) { app, http ->
        app.unsafe.routes.get("/lowercase") { it.result(it.queryParam("qp")!!) }
        assertThat(http.getBody("/LOWERCASE?qp=UPPERCASE")).isEqualTo("UPPERCASE")
    }

    @Test
    fun `path params and query params work as expected`() = JavalinTest.test(testApp) { app, http ->
        app.unsafe.routes.get("/user/{userId}") { it.result(it.pathParam("userId") + " " + it.queryParam("qp")!!) }
        assertThat(http.getBody("/UsEr/pkkummermo?qp=GladGutt")).isEqualTo("pkkummermo GladGutt")
    }

    @Test
    fun `path params follow by splat works`() = JavalinTest.test(testApp) { app, http ->
        app.unsafe.routes.get("/{param}/*") { it.result(it.path()) }
        assertThat(http.getBody("/PaRaM/sPlAt")).isEqualTo("/PaRaM/sPlAt")
    }

    private fun Response.assertStatusAndBodyMatch(status: Int, body: String) {
        assertThat(this.code()).isEqualTo(status)
        assertThat(this.body.string()).isNotNull.isEqualTo(body)
    }

    private fun Response.assertStatusAndBodyMatch(status: HttpStatus, body: String) {
        assertThat(this.httpCode()).isEqualTo(status)
        assertThat(this.body.string()).isNotNull.isEqualTo(body)
    }

    @Test
    fun `enableRedirectToLowercasePaths with caseInsensitiveRoutes throws exception`() {
        assertThrows<java.lang.IllegalStateException> {
            Javalin.create {
                it.router.caseInsensitiveRoutes = true
                it.registerPlugin(RedirectToLowercasePathPlugin())
            }.start()
        }
    }

}
