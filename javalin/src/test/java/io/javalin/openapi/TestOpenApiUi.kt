package io.javalin.openapi

import io.javalin.Javalin
import io.javalin.TestUtil
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.ui.ReDocOptions
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.swagger.v3.oas.models.info.Info
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


class TestOpenApiUi {
    private fun createDefaultOptions() = OpenApiOptions(
            Info().apply {
                title = "Example"
                version = "1.0.0"
            }
    )
            .path("/docs/swagger.json")

    @Test
    fun `should get swagger if activated`() {
        TestUtil.test(Javalin.create {
            it.registerPlugin(OpenApiPlugin(
                    createDefaultOptions()
                            .swagger(SwaggerOptions("/swagger"))
            ))
        }) { app, http ->
            assertThat(http.get("/swagger").body).contains("<html")
        }
    }

    @Test
    fun `should not get swagger if not activated`() {
        TestUtil.test(Javalin.create {
            it.registerPlugin(OpenApiPlugin(createDefaultOptions()))
        }) { app, http ->
            assertThat(http.get("/swagger").status).isEqualTo(404)
        }
    }

    @Test
    fun `should get reDoc if activated`() {
        TestUtil.test(Javalin.create {
            it.registerPlugin(OpenApiPlugin(
                    createDefaultOptions()
                            .reDoc(ReDocOptions("/redoc"))
            ))
        }) { app, http ->
            assertThat(http.get("/redoc").body).contains("<html")
        }
    }

    @Test
    fun `should not get reDoc if not activated`() {
        TestUtil.test(Javalin.create {
            it.registerPlugin(OpenApiPlugin(createDefaultOptions()))
        }) { app, http ->
            assertThat(http.get("/redoc").status).isEqualTo(404)
        }
    }
}
