package io.javalin

import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream

class TestBodyIsCreatedOnce {

    @Test
    fun `get body as byte array returns same instance`() {
        var firstCallBodyAsBytes = ByteArray(0)
        var secondCallBodyAsBytes = ByteArray(0)
        TestUtil.test { app, http ->
            app.post("/body-reader") { ctx ->
                firstCallBodyAsBytes = ctx.bodyAsBytes()
                secondCallBodyAsBytes = ctx.bodyAsBytes()
                ctx.result(ByteArrayInputStream(secondCallBodyAsBytes))
            }
            val httpResponse = http.post("/body-reader").body("my-body").asString()
            assertThat(httpResponse.status).isEqualTo(200)
            assertThat(httpResponse.body).isEqualTo("my-body")
            assertThat(firstCallBodyAsBytes).isSameAs(secondCallBodyAsBytes)
        }
    }

}
