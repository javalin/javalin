/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.openapi

import cc.vileda.openapi.dsl.asJson
import cc.vileda.openapi.dsl.components
import cc.vileda.openapi.dsl.externalDocs
import cc.vileda.openapi.dsl.get
import cc.vileda.openapi.dsl.info
import cc.vileda.openapi.dsl.openapiDsl
import cc.vileda.openapi.dsl.path
import cc.vileda.openapi.dsl.paths
import cc.vileda.openapi.dsl.security
import cc.vileda.openapi.dsl.securityScheme
import cc.vileda.openapi.dsl.server
import cc.vileda.openapi.dsl.tag
import com.mashape.unirest.http.Unirest
import io.javalin.Javalin
import io.javalin.TestUtil
import io.javalin.apibuilder.ApiBuilder.crud
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.Context
import io.javalin.plugin.openapi.JavalinOpenApi
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.annotations.HttpMethod
import io.javalin.plugin.openapi.dsl.OpenApiDocumentation
import io.javalin.plugin.openapi.dsl.document
import io.javalin.plugin.openapi.dsl.documentCrud
import io.javalin.plugin.openapi.dsl.documented
import io.javalin.plugin.openapi.dsl.documentedContent
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test

class Address(val street: String, val number: Int)

class User(val name: String, val address: Address? = null)

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
        securityScheme {
            type = SecurityScheme.Type.APIKEY
            `in` = SecurityScheme.In.COOKIE
            name = "token"
        }
    }
    security {
        addList("http")
        addList("token")
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

fun buildComplexExample(options: OpenApiOptions): Javalin {
    val app = Javalin.create {
        it.registerPlugin(OpenApiPlugin(options))
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
            .result("200", documentedContent<User>("application/xml"))
    app.get("/user", documented(getUserDocumentation) {
        it.json(User(name = "Jim"))
    })

    val getUserDocumentationSpecific = document()
            .operation {
                it.description = "Get a specific user with his/her id"
                it.summary = "Get specific user"
                it.operationId = "getSpecificUser"
            }
            .json<User>("200") {
                it.description = "Request successful"
            }
            .result("200", documentedContent<User>("application/xml"))

    app.get("/user/:userid", documented(getUserDocumentationSpecific) {
        it.json(User(name = it.pathParam("userid")))
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
            .body<User>("application/json")
            .body<User>("application/xml")
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

    app.get("/ignored", documented(document().ignore()) {})

    return app
}

data class MyError(val message: String)

class TestOpenApi {
    @Test
    fun `createSchema() work with complexExample and dsl`() {
        val app = buildComplexExample(OpenApiOptions(::createComplexExampleBaseConfiguration))
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
    fun `createSchema() work with extended crudhandler without documentation`() {
        val app = Javalin.create {}

        open class BaseCrudHandler : CrudHandler {
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

        open class ExtendedCrudHandler : BaseCrudHandler()

        class DoubleExtendedCrudHandler : ExtendedCrudHandler()

        app.routes {
            // Should not throw exception
            crud("users/:user-id", ExtendedCrudHandler())
            crud("accounts/:account-id", DoubleExtendedCrudHandler())
        }
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
                .defaultDocumentation { documentation ->
                    documentation.result<MyError>("500") {
                        it.description = "Server Error"
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
    fun `createSchema() apply defaults before actual documentation`() {
        val openApiOptions = OpenApiOptions(
                Info().title("Example").version("1.0.0")
        )
                .defaultDocumentation { documentation -> documentation.ignore() }
        val app = Javalin.create {
            it.registerPlugin(OpenApiPlugin(openApiOptions))
        }

        val route1Documentation = document()
                .ignore(false)
                .json<User>("200")

        val route3Documentation = document()
                .json<User>("200")

        with(app) {
            get("/route1", documented(route1Documentation) {})
            get("/route2") {}
            get("/route3", documented(route3Documentation) {})
        }

        val actual = JavalinOpenApi.createSchema(app)

        assertThat(actual.paths.containsKey("/route1")).isEqualTo(true)
        assertThat(actual.paths.containsKey("/route2")).isEqualTo(false)
        assertThat(actual.paths.containsKey("/route3")).isEqualTo(false)
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

    @Test
    fun `enableOpenApi() provide get route that allows cross-origin GET request`() {
        TestUtil.test(Javalin.create {
            it.registerPlugin(OpenApiPlugin(OpenApiOptions {
                OpenAPI().info(Info().apply {
                    title = "Example"
                    version = "1.0.0"
                })
            }.path("/docs/swagger.json")))
        }) { app, http ->
            app.get("/test") {}

            val actualHeaders = http.jsonGet("/docs/swagger.json").headers

            assertThat(actualHeaders.getFirst("Access-Control-Allow-Origin")).isEqualTo("*")
            assertThat(actualHeaders.getFirst("Access-Control-Allow-Methods")).isEqualTo("GET")
        }
    }

    @Test
    fun `setDocumentation() works`() {
        val app = Javalin.create {
            it.registerPlugin(
                    OpenApiPlugin(OpenApiOptions(Info().version("1.0.0").title("Override Example"))
                            .setDocumentation("/user", HttpMethod.POST, document().operation { operation ->
                                operation.description = "get description overwritten"
                            })
                            .setDocumentation("/user", HttpMethod.GET, document().operation { operation ->
                                operation.description = "post description overwritten"
                            }.result<User>("200"))
                    )
            )
        }

        app.get("/user", documented(document().operation {
            it.summary = "Summary"
            it.description = "test"
        }.result<String>("200")) {
            it.html("Not a user")
        })

        app.get("/unimplemented") {
        }

        val actual = JavalinOpenApi.createSchema(app)

        assertThat(actual.asJsonString()).isEqualTo(overrideJson)
    }

    @Test
    fun `setDocumentation() with non existing path works`() {
        val app = Javalin.create {
            it.registerPlugin(
                    OpenApiPlugin(OpenApiOptions(Info().version("1.0.0").title("Override Example"))
                            .setDocumentation("/user", HttpMethod.POST, document().operation { operation ->
                                operation.description = "get description overwritten"
                            })
                            .setDocumentation("/user", HttpMethod.GET, document().operation { operation ->
                                operation.description = "post description overwritten"
                            }.result<User>("200"))
                            .setDocumentation("/unimplemented", HttpMethod.GET, document())
                    )
            )
        }

        app.get("/user", documented(document().operation {
            it.summary = "Summary"
            it.description = "test"
        }.result<String>("200")) {
            it.html("Not a user")
        })

        val actual = JavalinOpenApi.createSchema(app)

        assertThat(actual.asJsonString()).isEqualTo(overrideJson)
    }

    @Test
    fun testOpenApiHandlerCaching() {
        val app = Javalin.create {
            it.registerPlugin(OpenApiPlugin(OpenApiOptions(::createComplexExampleBaseConfiguration).path("/openapi")))
        }

        TestUtil.test(app) { _, http ->
            // Generate OpenApi-Schema
            Unirest.get("${http.origin}/openapi").asString()

            // Get cached OpenApi-Schema
            val cachedStartTime = System.nanoTime()
            Unirest.get("${http.origin}/openapi").asString()
            val cachedRequestTime = System.nanoTime() - cachedStartTime

            // Initializing the OpenApi schema is slow (map to yml, parse, validate)
            // It should run loner than 100 ms, the cached version should be faster than 100ms
            assert(cachedRequestTime / 1000 / 1000 < 100)
        }
    }

    @Test
    fun `ignorePath() works`() {
        val app = buildComplexExample(OpenApiOptions(::createComplexExampleBaseConfiguration).ignorePath("/user"))
        val actual = JavalinOpenApi.createSchema(app)
        val json = actual.asJson().getJSONObject("paths")
        val userJson = json.getJSONObject("/user").toString()
        val userWithIdJson = json.getJSONObject("/user/{userid}").toString()

        assertThat(userJson).isEqualTo("{\"description\":\"Some additional information for the /user endpoint\"}")
        assertThat(userWithIdJson.formatJson()).isEqualTo(userWithIdJsonExpected.formatJson())
    }

    @Test
    fun `ignorePath() with http methods works`() {
        val app = buildComplexExample(OpenApiOptions(::createComplexExampleBaseConfiguration)
                .ignorePath("/user", HttpMethod.PUT))
        val actual = JavalinOpenApi.createSchema(app)
        val json = actual.asJson().getJSONObject("paths").getJSONObject("/user").toString()

        assertThat(json.formatJson()).isEqualTo(userJsonExpected.formatJson())
    }

    @Test
    fun `ignorePath() with prefix works`() {
        val app = buildComplexExample(OpenApiOptions(::createComplexExampleBaseConfiguration)
                .ignorePath("/user*"))
        val actual = JavalinOpenApi.createSchema(app)
        val json = actual.asJson().getJSONObject("paths")
        val userJson = json.getJSONObject("/user").toString()

        assertThat(!json.has("/user/{userid}"))
        assertThat(userJson).isEqualTo("{\"description\":\"Some additional information for the /user endpoint\"}")
    }

    @Test
    fun `ignorePath() with prefix and http methods works`() {
        val app = buildComplexExample(OpenApiOptions(::createComplexExampleBaseConfiguration)
                .ignorePath("/user*", HttpMethod.PUT))
        val actual = JavalinOpenApi.createSchema(app)
        val json = actual.asJson().getJSONObject("paths")
        val userJson = json.getJSONObject("/user").toString()
        val userWithIdJson = json.getJSONObject("/user/{userid}").toString()

        assertThat(userJson.formatJson()).isEqualTo(userJsonExpected.formatJson())
        assertThat(userWithIdJson.formatJson()).isEqualTo(userWithIdJsonExpected.formatJson())
    }
}
