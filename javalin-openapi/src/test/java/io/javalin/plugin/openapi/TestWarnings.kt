package io.javalin.plugin.openapi

import io.javalin.Javalin
import io.javalin.plugin.openapi.utils.VersionIssuesUtil
import io.javalin.testing.TestUtil
import io.swagger.v3.oas.models.OpenAPI
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestWarnings {

    @Test
    fun `should log for new versions of java and kotlin that break reflection`() {
        val log = TestUtil.captureStdOut {
            Javalin.create {
                it.registerPlugin(OpenApiPlugin(OpenApiOptions { OpenAPI() }))
            }
        }
        if (VersionIssuesUtil.warning != null) {
            assertThat(log).contains(VersionIssuesUtil.warning)
        }
    }

}
