package io.javalin.plugin.openapi

import io.javalin.Javalin
import io.javalin.plugin.openapi.dsl.document
import io.javalin.plugin.openapi.dsl.documented
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestOpenApiIncludePath {
    @Test
    /**
     * The aim of this test is to show that using the "includePath" method correctly only includes the selected paths
     */
    fun testIncludedMethods() {
        val defaultApp = Javalin.create {
            it.registerPlugin(
                OpenApiPlugin(
                    OpenApiOptions {
                        OpenAPI().info(Info().title("Example").version("1.0.0"))
                    }
                )
            )
        }
        defaultApp.get("path1", documented(path1Documentation()) { it -> it.result("path1") })
        defaultApp.get("path2", documented(path2Documentation()) { it -> it.result("path2") })
        defaultApp.get("path3", documented(path3Documentation()) { it -> it.result("path3") })

        val includedApp = Javalin.create {
            it.registerPlugin(
                OpenApiPlugin(
                    OpenApiOptions {
                        OpenAPI().info(Info().title("Example").version("1.0.0"))
                    }
                    .includePath("path1")
                    .includePath("path2")
                )
            )
        }
        includedApp.get("path1", documented(path1Documentation()) { it -> it.result("path1") })
        includedApp.get("path2", documented(path2Documentation()) { it -> it.result("path2") })
        includedApp.get("path3", documented(path3Documentation()) { it -> it.result("path2") })

        val defaultSchema = JavalinOpenApi.createSchema(defaultApp).toString().toLowerCase()
        val includedSchema = JavalinOpenApi.createSchema(includedApp).toString().toLowerCase()

        assertThat(defaultSchema).isNotEqualTo(includedSchema)
        //the default schema contains no inclusion criteria so should contain paths for path1, 2 and 3
        assertThat(defaultSchema).contains("/path1=class PathItem".toLowerCase())
        assertThat(defaultSchema).contains("/path2=class PathItem".toLowerCase())
        assertThat(defaultSchema).contains("/path3=class PathItem".toLowerCase())

        //the included schema has include paths for path1 and path 2 so should only contain them and not path 3
        assertThat(includedSchema).contains("/path1=class PathItem".toLowerCase())
        assertThat(includedSchema).contains("/path2=class PathItem".toLowerCase())
        assertThat(includedSchema).doesNotContain("/path3=class PathItem".toLowerCase())

    }

    private fun path1Documentation() = document().operation { it.summary="path1" }.result<String>("200")
    private fun path2Documentation() = document().operation { it.summary="path2" }.result<String>("200")
    private fun path3Documentation() = document().operation { it.summary="path3" }.result<String>("200")
}
