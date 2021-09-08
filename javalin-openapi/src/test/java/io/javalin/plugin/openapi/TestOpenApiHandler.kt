package io.javalin.plugin.openapi

import io.javalin.Javalin
import io.javalin.plugin.openapi.dsl.document
import io.javalin.plugin.openapi.dsl.documented
import io.swagger.v3.oas.models.info.Info
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class TestOpenApiHandler {

    private val expectedJson = """
        {
          "openapi" : "3.0.1",
          "info" : {
            "title" : "testApp",
            "version" : "1.0"
          },
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
                          "$ref" : "#/components/schemas/TestUser"
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
                          "$ref" : "#/components/schemas/TestAddress"
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
              "TestAddress" : {
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
              "TestUser" : {
                "required" : [ "address", "name" ],
                "type" : "object",
                "properties" : {
                  "name" : {
                    "type" : "string"
                  },
                  "address" : {
                    "$ref" : "#/components/schemas/TestAddress"
                  }
                }
              }
            }
          }
        }
    """.trimIndent()

    private data class TestUser(val name: String, val address: TestAddress)
    private data class TestAddress(val street: String, val number: Int)

    private fun createApp(openApiPlugin: OpenApiPlugin) = Javalin.create {
        it.registerPlugin(openApiPlugin)
    }.apply {
        get("/user", documented(document().result<TestUser>("200")) {})
        get("/address", documented(document().result<TestAddress>("200")) {})
    }

    @Test
    fun `schema validates without warning about unexpected exampleSetFlag`() {
        // title and version required for validation
        val info = Info().title("testApp").version("1.0")

        // enable validation
        val app = createApp(OpenApiPlugin(OpenApiOptions(info).apply {
            validateSchema(true)
        }))

        // generate the schema
        val openApiSchema = JavalinOpenApi.createSchema(app)

        // re-validate the schema to verify no error messages were generated
        val handler = app._conf.getPlugin(OpenApiPlugin::class.java).openApiHandler
        val validated = handler.validateOpenAPISchema(openApiSchema)

        Assertions.assertThat(validated.messages).isEmpty()
        Assertions.assertThat(openApiSchema.asJsonString()).isEqualTo(expectedJson.formatJson())
    }

}
