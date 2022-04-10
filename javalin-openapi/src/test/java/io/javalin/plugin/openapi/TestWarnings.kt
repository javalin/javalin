package io.javalin.plugin.openapi

import io.javalin.Javalin
import io.javalin.plugin.openapi.utils.OpenApiVersionUtil
import io.javalin.testing.TestUtil
import io.swagger.v3.oas.models.OpenAPI
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestWarnings {

    @Test
    fun `should log for new versions of java and kotlin that break reflection`() {
        OpenApiVersionUtil.logWarnings = true
        val log = TestUtil.captureStdOut {
            Javalin.create {
                it.registerPlugin(OpenApiPlugin(OpenApiOptions { OpenAPI() }))
            }
        }
        if (OpenApiVersionUtil.warning != null) {
            assertThat(log).contains(OpenApiVersionUtil.warning)
        }
    }

    @Test
    fun `should not log if warning disabled`() {
        OpenApiVersionUtil.logWarnings = false
        val log = TestUtil.captureStdOut {
            Javalin.create {
                it.registerPlugin(OpenApiPlugin(OpenApiOptions { OpenAPI() }))
            }
        }
        if (OpenApiVersionUtil.warning != null) {
            assertThat(log).doesNotContain(OpenApiVersionUtil.warning)
        }
    }

}
