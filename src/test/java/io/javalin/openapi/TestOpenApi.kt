/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.openapi

import cc.vileda.openapi.dsl.components
import cc.vileda.openapi.dsl.externalDocs
import cc.vileda.openapi.dsl.get
import cc.vileda.openapi.dsl.info
import cc.vileda.openapi.dsl.openapiDsl
import cc.vileda.openapi.dsl.path
import cc.vileda.openapi.dsl.paths
import cc.vileda.openapi.dsl.response
import cc.vileda.openapi.dsl.responses
import cc.vileda.openapi.dsl.security
import cc.vileda.openapi.dsl.securityScheme
import cc.vileda.openapi.dsl.server
import cc.vileda.openapi.dsl.tag
import io.javalin.Javalin
import io.javalin.TestUtil
import io.javalin.apibuilder.ApiBuilder.crud
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.Context
import io.javalin.plugin.openapi.JavalinOpenApi
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.dsl.OpenApiDocumentation
import io.javalin.plugin.openapi.dsl.document
import io.javalin.plugin.openapi.dsl.documentCrud
import io.javalin.plugin.openapi.dsl.documented
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test

class User(val name: String)

fun createComplexExampleBaseConfiguration() = openapiDsl {
    info {
        title = "Example"
        version = "1.0.0"
    }
    server {
        url = "https://app.example"
        description = "My example app"
    }
    paths {
        path("/unimplemented") {
            get {
                summary = "This path is not implemented in javalin"
            }
        }
        path("/user") {
            description = "Some additional information for the /user endpoint"
        }
    }
    components {
        securityScheme {
            type = SecurityScheme.Type.HTTP
            scheme = "basic"
        }
    }
    security {
        addList("http")
    }
    tag {
        name = "user"
        description = "User operations"
    }
    externalDocs {
        description = "Find more info here"
        url = "https://external-documentation.info"
    }
}

class TestOpenApi {
    @Test
    fun `createSchema() work with complexExample and dsl`() {
        val app = Javalin.create {
            it.registerPlugin(OpenApiPlugin(OpenApiOptions(::createComplexExampleBaseConfiguration)))
        }

        val getUserDocumentation = document()
                .operation {
                    it.description = "Get a specific user"
                    it.summary = "Get current user"
                    it.operationId = "getCurrentUser"
                    it.deprecated = true
                    it.addTagsItem("user")
                }
                .json<User>("200") {
                    it.description = "Request successful"
                }
        app.get("/user", documented(getUserDocumentation) {
            it.json(User(name = "Jim"))
        })

        val getUsersDocumentation = document()
                .operation {
                    it.addTagsItem("user")
                }
                .cookie<String>("my-cookie") {
                    it.description = "My cookie"
                }
                .header<String>("x-my-header") {
                    it.description = "My header"
                }
                .pathParam<Int>("my-path-param") {
                    it.description = "My path param"
                }
                .queryParam<String>("name") {
                    it.description = "The name of the users you want to filter"
                    it.required = true
                    it.deprecated = true
                    it.allowEmptyValue = true
                }
                .queryParam<Int>("age")
                .jsonArray<User>("200")
        app.get("/users/:my-path-param", documented(getUsersDocumentation) {
            val myCookie = it.cookie("my-cookie")
            val myHeader = it.cookie("x-my-header")
            val nameFilter = it.queryParam("name")
            val ageFilter = it.queryParam("age")
            it.json(listOf(User(name = "Jim")))
        })

        val getUsers2Documentation = document()
                .operation {
                    it.addTagsItem("user")
                }
                .json<Array<User>>("200")
        app.get("/users2", documented(getUsers2Documentation) { it.json(listOf(User(name = "Jim"))) })

        val putUserDocumentation = document()
                .operation {
                    it.addTagsItem("user")
                }
                .body<String> {
                    it.description = "body description"
                    it.required = true
                }
                .body<User>()
                .bodyAsBytes()
                .bodyAsBytes("image/png")
        app.put("/user", documented(putUserDocumentation) {
            val userString = it.body()
            val user = it.bodyAsClass(User::class.java)
            val userAsBytes = it.bodyAsBytes()
            val userImage = it.bodyAsBytes()
        })

        val getStringDocumentation = OpenApiDocumentation()
                .result<String>("200")
        app.get("/string", documented(getStringDocumentation) {
            it.result("Hello")
        })

        val getHomepageDocumentation = OpenApiDocumentation()
                .html("200") {
                    it.description = "My Homepage"
                }
        app.get("/homepage", documented(getHomepageDocumentation) {
            it.html("<p>Hello World</p>")
        })

        val getUploadDocumentation = OpenApiDocumentation()
                .uploadedFile("file") {
                    it.description = "MyFile"
                    it.required = true
                }
        app.get("/upload", documented(getUploadDocumentation) {
            it.uploadedFile("file")
        })

        val getUploadsDocumentation = OpenApiDocumentation()
                .uploadedFiles("files") {
                    it.description = "MyFiles"
                    it.required = true
                }
        app.get("/uploads", documented(getUploadsDocumentation) {
            it.uploadedFiles("files")
        })

        val getResourcesDocumentation = OpenApiDocumentation()
                .result<Unit>("200")
        app.get("/resources/*", documented(getResourcesDocumentation) {})

        val actual = JavalinOpenApi.createSchema(app)

        assertThat(actual.asJsonString()).isEqualTo(complexExampleJson)
    }

    @Test
    fun `createSchema() work with crudHandler and dsl`() {
        val app = Javalin.create {
            it.registerPlugin(OpenApiPlugin(OpenApiOptions { OpenAPI().info(Info().title("Example").version("1.0.0")) }))
        }

        val userCrudHandlerDocumentation = documentCrud()
                .getOne(document().json<User>("200"))
                .getAll(document().jsonArray<User>("200"))


        class UserCrudHandler : CrudHandler {
            override fun getAll(ctx: Context) {
            }

            override fun getOne(ctx: Context, resourceId: String) {
            }

            override fun create(ctx: Context) {
            }

            override fun update(ctx: Context, resourceId: String) {
            }

            override fun delete(ctx: Context, resourceId: String) {
            }
        }

        app.routes {
            crud("users/:user-id", documented(userCrudHandlerDocumentation, UserCrudHandler()))
        }

        val actual = JavalinOpenApi.createSchema(app)

        assertThat(actual.asJsonString()).isEqualTo(crudExampleJson)
    }

    @Test
    fun `createSchema() throw error if enableOpenApi is not activated`() {
        val app = Javalin.create()

        val getUserDocumentation = document().result<User>("200")
        app.get("/user", documented(getUserDocumentation) { it.json(User(name = "Jim")) })

        app.put("/user") {}

        assertThatExceptionOfType(IllegalStateException::class.java)
                .isThrownBy {
                    JavalinOpenApi.createSchema(app)
                }
                .withMessage("You need to register the \"OpenApiPlugin\" before you can create the OpenAPI schema")
    }

    @Test
    fun `createSchema() work with default operation`() {
        val openApiOptions = OpenApiOptions(
                Info().title("Example").version("1.0.0")
        )
                .defaultOperation { operation, _ ->
                    operation.responses {
                        response("500") { description = "Server Error" }
                    }
                }
        val app = Javalin.create {
            it.registerPlugin(OpenApiPlugin(openApiOptions))
        }

        val route2Documentation = document()
                .json<User>("200")

        with(app) {
            get("/route1", documented(route2Documentation) {})
            get("/route2") {}
        }

        val actual = JavalinOpenApi.createSchema(app)

        assertThat(actual.asJsonString()).isEqualTo(defaultOperationExampleJson)
    }

    @Test
    fun `enableOpenApi() provide get route if path is given`() {
        TestUtil.test(Javalin.create {
            it.registerPlugin(OpenApiPlugin(OpenApiOptions(
                    Info().apply {
                        title = "Example"
                        version = "1.0.0"
                    }
            ).path("/docs/swagger.json")))
        }) { app, http ->
            app.get("/test") {}

            val actual = http.jsonGet("/docs/swagger.json").body

            assertThat(actual).isEqualTo(provideRouteExampleJson)
        }
    }

    @Test
    fun `enableOpenApi() provide get route if path is given with baseConfiguration`() {
        TestUtil.test(Javalin.create {
            it.registerPlugin(OpenApiPlugin(OpenApiOptions {
                OpenAPI().info(Info().apply {
                    title = "Example"
                    version = "1.0.0"
                })
            }.path("/docs/swagger.json")))
        }) { app, http ->
            app.get("/test") {}

            val actual = http.jsonGet("/docs/swagger.json").body

            assertThat(actual).isEqualTo(provideRouteExampleJson)
        }
    }
}
