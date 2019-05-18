/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.openapi

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.crud
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.openapi.JavalinOpenApi
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.annotations.ContentType
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiFileUpload
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import org.junit.Test

// region complexExampleWithAnnotationsHandler
@OpenApi(
        description = "Get a specific user",
        summary = "Get current user",
        operationId = "getCurrentUser",
        deprecated = true,
        tags = ["user"],
        responses = [
            OpenApiResponse(status = "200", returnType = User::class, description = "Request successful")
        ]
)
fun getUserHandler(ctx: Context) {
}

@OpenApi(
        tags = ["user"],
        cookies = [
            OpenApiParam(name = "my-cookie", type = String::class, description = "My cookie")
        ],
        headers = [
            OpenApiParam(name = "x-my-header", type = String::class, description = "My header")
        ],
        pathParams = [
            OpenApiParam(name = "my-path-param", type = Int::class, description = "My path param")
        ],
        queryParams = [
            OpenApiParam(
                    name = "name",
                    type = String::class,
                    description = "The name of the users you want to filter",
                    required = true,
                    deprecated = true,
                    allowEmptyValue = true
            ),
            OpenApiParam(name = "age", type = Int::class)
        ],
        responses = [
            OpenApiResponse(status = "200", returnType = User::class, isArray = true)
        ]
)
fun getUsersHandler(ctx: Context) {
}

@OpenApi(
        tags = ["user"],
        responses = [
            OpenApiResponse(status = "200", returnType = Array<User>::class)
        ]
)
fun getUsers2Handler(ctx: Context) {
}

@OpenApi(
        tags = ["user"],
        requestBodies = [
            OpenApiRequestBody(type = String::class, required = true, description = "body description"),
            OpenApiRequestBody(type = User::class),
            OpenApiRequestBody(type = ByteArray::class),
            OpenApiRequestBody(type = ByteArray::class, contentType = "image/png")
        ]
)
fun putUserHandler(ctx: Context) {
}

@OpenApi(
        responses = [
            OpenApiResponse(status = "200", returnType = String::class)
        ]
)
fun getStringHandler(ctx: Context) {
}

@OpenApi(
        responses = [
            OpenApiResponse(status = "200", contentType = ContentType.HTML, description = "My Homepage")
        ]
)
fun getHomepageHandler(ctx: Context) {
}

@OpenApi(
        fileUploads = [
            OpenApiFileUpload(name = "file", description = "MyFile", required = true)
        ]
)
fun getUploadHandler(ctx: Context) {
}

@OpenApi(
        fileUploads = [
            OpenApiFileUpload(name = "files", description = "MyFiles", isArray = true, required = true)
        ]
)
fun getUploadsHandler(ctx: Context) {
}

@OpenApi(
        responses = [
            OpenApiResponse(status = "200")
        ]
)
fun getResources(ctx: Context) {
}
// endregion complexExampleWithAnnotationsHandler
// region handler types
class ClassHandler : Handler {
    @OpenApi(responses = [OpenApiResponse(status = "200")])
    override fun handle(ctx: Context) {
    }
}

@OpenApi(responses = [OpenApiResponse(status = "200")])
fun kotlinFunctionHandler(ctx: Context) {
}

object KotlinFieldHandlers {
    @OpenApi(responses = [OpenApiResponse(status = "200")])
    var kotlinFieldHandler = Handler { ctx -> }
}

// endregion handler types

class TestOpenApiAnnotations {
    @Test
    fun `createOpenApiSchema() work with complexExample and annotations`() {
        val app = Javalin.create {
            it.registerPlugin(OpenApiPlugin(OpenApiOptions(::createComplexExampleBaseConfiguration)))
        }

        app.get("/user", ::getUserHandler)
        app.get("/users/:my-path-param", ::getUsersHandler)
        app.get("/users2", ::getUsers2Handler)
        app.put("/user", ::putUserHandler)
        app.get("/string", ::getStringHandler)
        app.get("/homepage", ::getHomepageHandler)
        app.get("/upload", ::getUploadHandler)
        app.get("/uploads", ::getUploadsHandler)
        app.get("/resources/*", ::getResources)

        val actual = JavalinOpenApi.createSchema(app)

        actual.assertEqualTo(complexExampleJson)
    }

    @Test
    fun `createOpenApiSchema() work with crudHandler and annotations`() {
        class UserCrudHandlerWithAnnotations : CrudHandler {
            @OpenApi(
                    responses = [
                        OpenApiResponse("200", returnType = User::class, isArray = true)
                    ]
            )
            override fun getAll(ctx: Context) {
            }

            @OpenApi(
                    responses = [
                        OpenApiResponse("200", returnType = User::class)
                    ]
            )
            override fun getOne(ctx: Context, resourceId: String) {
            }

            override fun create(ctx: Context) {
            }

            override fun update(ctx: Context, resourceId: String) {
            }

            override fun delete(ctx: Context, resourceId: String) {
            }
        }

        val actual = extractSchemaForTest { app ->
            app.routes { crud("users/:user-id", UserCrudHandlerWithAnnotations()) }
        }

        actual.assertEqualTo(crudExampleJson)
    }

    @Test
    fun `createOpenApiSchema() with class`() {
        extractSchemaForTest {
            it.get("/test", ClassHandler())
        }.assertEqualTo(simpleExample)
    }

    @Test
    fun `createOpenApiSchema() with kotlin function`() {
        extractSchemaForTest {
            it.get("/test", ::kotlinFunctionHandler)
        }.assertEqualTo(simpleExample)
    }

    @Test
    fun `createOpenApiSchema() with kotlin field`() {
        extractSchemaForTest {
            it.get("/test", KotlinFieldHandlers.kotlinFieldHandler)
        }.assertEqualTo(simpleExample)
    }
}
