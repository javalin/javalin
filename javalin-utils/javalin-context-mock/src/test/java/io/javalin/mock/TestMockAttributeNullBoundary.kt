package io.javalin.mock

import io.javalin.Javalin
import jakarta.servlet.http.HttpServletRequest
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TestMockAttributeNullBoundary {

    @Test
    fun `servlet interface accepts null to remove a request attribute`() {
        ContextMock.create().execute { ctx ->
            val request: HttpServletRequest = ctx.req()
            request.setAttribute("temporary", "value")
            request.setAttribute("temporary", null)
            assertThat(request.getAttribute("temporary")).isNull()
            assertThat(request.attributeNames.toList()).doesNotContain("temporary")
        }
    }

    @Test
    fun `context attribute null removes an existing attribute`() {
        ContextMock.create().execute { ctx ->
            ctx.attribute("temporary", "value")
            ctx.attribute("temporary", null)
            assertThat(ctx.attribute<String>("temporary")).isNull()
            assertThat(ctx.attributeMap()).doesNotContainKey("temporary")
        }
    }

    @Test
    fun `context attribute null for an absent key is a no-op`() {
        ContextMock.create().execute { ctx ->
            ctx.attribute("kept", "other value")
            ctx.attribute("absent", null)
            assertThat(ctx.attributeMap()).containsEntry("kept", "other value").doesNotContainKey("absent")
        }
    }

    @Test
    fun `non-null attributes can still be replaced`() {
        ContextMock.create().execute { ctx ->
            ctx.attribute("key", "first")
            ctx.attribute("key", "second")
            assertThat(ctx.attribute<String>("key")).isEqualTo("second")
        }
    }

    @Test
    fun `real Jetty accepts the same null attribute flow`() {
        val app = Javalin.create { config ->
            config.routes.get("/") { ctx ->
                ctx.attribute("temporary", "value")
                ctx.attribute("temporary", null)
                ctx.result(if (ctx.attribute<String>("temporary") == null && !ctx.attributeMap().containsKey("temporary")) "removed" else "retained")
            }
        }.start(0)
        val client = Unirest.spawnInstance()
        try {
            val response = client.get("http://localhost:${app.port()}/").asString()
            assertThat(response.status).isEqualTo(200)
            assertThat(response.body).isEqualTo("removed")
        } finally {
            client.shutDown()
            app.stop()
        }
    }
}
