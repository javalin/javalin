package io.javalin.plugin.openapi

import io.javalin.Javalin
import io.javalin.plugin.openapi.dsl.document
import io.javalin.plugin.openapi.dsl.documented
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.info.Info
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

private data class ExampleUser(val name: String, val address: ExampleAddress)
private data class ExampleAddress(val street: String, val number: Int)

val examples = mapOf(
        ExampleUser::class.java to mapOf(
                "User example" to Example().apply {
                    summary = "A correctly configured user"
                    value = ExampleUser("John", ExampleAddress("Some street", 123))
                },
                "User example 2" to Example().apply {
                    summary = "Another correctly configured user"
                    value = ExampleUser("Dave", ExampleAddress("Some street", 123))
                }
        ),
        ExampleAddress::class.java to mapOf(
                "Address example" to Example().apply {
                    summary = "A correctly configured address"
                    value = ExampleAddress("Some street", 123)
                }
        )
)

val expectedJson = """
    {
      "openapi" : "3.0.1",
      "info" : { },
      "paths" : {
        "/user" : {
          "get" : {
            "summary" : "Get user",
            "operationId" : "getUser",
            "responses" : {
              "200" : {
                "description" : "OK",
                "content" : {
                  "application/json" : {
                    "schema" : {
                      "$ref" : "#/components/schemas/ExampleUser"
                    },
                    "examples" : {
                      "User example" : {
                        "summary" : "A correctly configured user",
                        "value" : {
                          "name" : "John",
                          "address" : {
                            "street" : "Some street",
                            "number" : 123
                          }
                        }
                      },
                      "User example 2" : {
                        "summary" : "Another correctly configured user",
                        "value" : {
                          "name" : "Dave",
                          "address" : {
                            "street" : "Some street",
                            "number" : 123
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        },
        "/address" : {
          "get" : {
            "summary" : "Get address",
            "operationId" : "getAddress",
            "responses" : {
              "200" : {
                "description" : "OK",
                "content" : {
                  "application/json" : {
                    "schema" : {
                      "$ref" : "#/components/schemas/ExampleAddress"
                    },
                    "examples" : {
                      "Address example" : {
                        "summary" : "A correctly configured address",
                        "value" : {
                          "street" : "Some street",
                          "number" : 123
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      },
      "components" : {
        "schemas" : {
          "ExampleAddress" : {
            "required" : [ "number", "street" ],
            "type" : "object",
            "properties" : {
              "street" : {
                "type" : "string"
              },
              "number" : {
                "type" : "integer",
                "format" : "int32"
              }
            }
          },
          "ExampleUser" : {
            "required" : [ "address", "name" ],
            "type" : "object",
            "properties" : {
              "name" : {
                "type" : "string"
              },
              "address" : {
                "$ref" : "#/components/schemas/ExampleAddress"
              }
            }
          }
        }
      }
    }
""".trimIndent()

class TestOpenApiExample {

    private fun createApp(openApiPlugin: OpenApiPlugin) = Javalin.create {
        it.registerPlugin(openApiPlugin)
    }.apply {
        get("/user", documented(document().result<ExampleUser>("200")) {})
        get("/address", documented(document().result<ExampleAddress>("200")) {})
    }

    @Test
    fun `examples are generated when added in addExample, and class can have multiple examples`() {
        val app = createApp(OpenApiPlugin(OpenApiOptions(Info()).apply {
            addExample<ExampleUser>("User example", examples[ExampleUser::class.java]!!["User example"]!!)
            addExample<ExampleUser>("User example 2", examples[ExampleUser::class.java]!!["User example 2"]!!)
            addExample<ExampleAddress>("Address example", examples[ExampleAddress::class.java]!!["Address example"]!!)
        }))

        val openApiJson = JavalinOpenApi.createSchema(app).asJsonString()

        assertThat(openApiJson).isEqualTo(expectedJson.formatJson())
        openApiExamples.clear()
    }

    @Test
    fun `examples are generated when added as a map`() {
        val app = createApp(OpenApiPlugin(OpenApiOptions(Info()).apply {
            examples(examples)
        }))

        val openApiJson = JavalinOpenApi.createSchema(app).asJsonString()

        assertThat(openApiJson).isEqualTo(expectedJson.formatJson())
        openApiExamples.clear()
    }

}
