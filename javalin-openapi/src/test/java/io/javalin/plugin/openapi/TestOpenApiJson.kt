package io.javalin.plugin.openapi

import io.javalin.Javalin
import io.javalin.examples.addUserDocs
import io.javalin.examples.addUserHandler
import io.javalin.examples.getUserDocs
import io.javalin.examples.getUserHandler
import io.javalin.examples.getUsersDocs
import io.javalin.examples.getUsersHandler
import io.javalin.http.InternalServerErrorResponse
import io.javalin.plugin.openapi.dsl.documented
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.javalin.testing.TestUtil
import io.swagger.v3.oas.models.info.Info
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class TestOpenApiJson {

    companion object {
        fun createDocumentedJavalin(): Javalin {
            val app = Javalin.create {
                val openApiOptions = OpenApiOptions(
                        Info().version("1.0").description("My Application"))
                        .path("/swagger-docs")
                        .swagger(SwaggerOptions("/swagger").title("My Swagger Documentation"))
                        .defaultDocumentation { documentation -> documentation.json<InternalServerErrorResponse>("500") }
                it.registerPlugin(OpenApiPlugin(openApiOptions))
            }

            with(app) {
                get("/users", documented(getUsersDocs, ::getUsersHandler))
                get("/users/{id}", documented(getUserDocs, ::getUserHandler))
                post("/users", documented(addUserDocs, ::addUserHandler))
            }

            return app
        }
    }

    /**
     * A minor test to verify, the OpenAPI Json was generated
     */
    @Test
    fun testOpenApiJsonIsAvailable() = TestUtil.test(createDocumentedJavalin()){ app, http ->
        val resp = http.get("/swagger")
        // Generally: Is the openapi json available
        Assertions.assertThat(resp.status == 200)
        // Sanity check: JSON like?
        Assertions.assertThat(resp.body.startsWith("{"))
        Assertions.assertThat(resp.body.endsWith("}"))
        // Basic assertions towards openapi json
        Assertions.assertThat(resp.body.contains("My Application"))
        Assertions.assertThat(resp.body.contains("openapi"))
        Assertions.assertThat(resp.body.contains("paths"))
        Assertions.assertThat(resp.body.contains("schema"))
    }

    /**
     * Verifying that there is no exampleSetFlag in the json
     */
    @Test fun testOpenApiJsonNotContainsExampleSetFlag() = TestUtil.test(createDocumentedJavalin()){ app, http->
        val resp = http.get("/swagger")
        // Generally: Is the openapi json available
        Assertions.assertThat(resp.status == 200)
        // Sanity check: JSON like?
        Assertions.assertThat(resp.body.startsWith("{"))
        Assertions.assertThat(resp.body.endsWith("}"))

        // No internal property exposed such as 'exampleFlagSet'
        Assertions.assertThat(!resp.body.contains("exampleFlagSet"))
    }


}
