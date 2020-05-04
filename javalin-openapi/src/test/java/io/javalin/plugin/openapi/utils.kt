@file:JvmName("OpenApiTestUtils")

package io.javalin.plugin.openapi;

import io.javalin.Javalin
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.assertj.core.api.Assertions.assertThat

fun extractSchemaForTest(initSchema: (app: Javalin) -> Unit): OpenAPI {
    val options = OpenApiOptions(Info().title("Example").version("1.0.0"))
    return extractSchemaForTest(options, initSchema)
}

fun extractSchemaForTest(options: OpenApiOptions, initSchema: (app: Javalin) -> Unit): OpenAPI {
    val app = Javalin.create { it.registerPlugin(OpenApiPlugin(options)) }
    initSchema(app)
    return JavalinOpenApi.createSchema(app)
}


fun OpenAPI.assertEqualTo(expectedSchemaJson: String) {
    assertThat(this.asJsonString()).isEqualTo(expectedSchemaJson.formatJson())
}
