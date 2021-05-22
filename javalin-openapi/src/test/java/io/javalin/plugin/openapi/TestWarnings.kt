package io.javalin.plugin.openapi

import io.javalin.Javalin
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
        val javaVersion = System.getProperty("java.version").split(".")[0].toInt()
        val kotlinVersion = KotlinVersion.CURRENT.minor // let's face it, to JetBrains minor means major
        val warning = when {
            javaVersion >= 15 && kotlinVersion >= 5 -> "JDK15 and Kotlin 1.5 break reflection in different ways"
            javaVersion >= 15 -> "JDK 15 has a breaking change to reflection"
            kotlinVersion >= 5 -> "Kotlin 1.5 has a breaking change to reflection"
            else -> null
        }
        if (warning != null) {
            assertThat(log).contains(warning)
        }
    }

}
