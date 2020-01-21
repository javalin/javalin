package io.javalin.openapi

import io.javalin.Javalin
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.info.Info
import org.junit.Test

private data class ExampleUser(val name: String, val address: Address)
private data class ExampleAddress(val street: String, val number: Int)

val examples = mapOf(
        ExampleUser::class.java to mapOf(
                "User example" to Example().apply {
                    summary = "A correctly configured user"
                    value = User("John", Address("Some street", 123))
                }
        ),
        ExampleAddress::class.java to mapOf(
                "Address example" to Example().apply {
                    summary = "A correctly configured address"
                    value = Address("Some street", 123)
                }
        )
)

class TestOpenApiExample {

    @Test
    fun `examples are generated when added in addExampleForSchema`() {
        val app = Javalin.create { config ->
            config.registerPlugin(OpenApiPlugin(OpenApiOptions(Info()).apply {
                addExampleForSchema<ExampleUser>("User example", examples[ExampleUser::class.java]!!["User example"]!!)
                addExampleForSchema<ExampleUser>("Address example", examples[ExampleAddress::class.java]!!["Address example"]!!)
            }))
        }
        // TODO: What's the easiest way of testing this?
    }

    @Test
    fun `examples are generated when added as a map`() {
        val app = Javalin.create { config ->
            config.registerPlugin(OpenApiPlugin(OpenApiOptions(Info()).apply {
                examples(examples)
            }))
        }
        // TODO: What's the easiest way of testing this?
    }

}


