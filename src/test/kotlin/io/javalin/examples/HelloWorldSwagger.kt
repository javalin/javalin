/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.examples

import cc.vileda.openapi.dsl.response
import cc.vileda.openapi.dsl.responses
import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.dsl.document
import io.javalin.plugin.openapi.dsl.documented
import io.javalin.plugin.openapi.ui.ReDocOptions
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.swagger.v3.oas.models.info.Info

enum class UserRole {
    CUSTOMER,
    ADMIN
}

data class User(val id: Int, val name: String, val age: Int, val role: UserRole)

object UserRepository {
    private val users = mutableListOf<User>()
    fun getUsers() = users.toList()
    fun getUser(id: Int) = users.find { it.id == id }
    fun addUser(user: User) = users.add(user)
}

val getUsersDocs = document().jsonArray<User>("200") { it.description = "Returns all users" }

fun getUsersHandler(ctx: Context) {
    val users = UserRepository.getUsers()
    ctx.json(users)
}

val getUserDocs = document()
        .pathParam<Int>("id")
        .json<User>("200") { it.description = "Returns user with id" }
        .result<Unit>("404")

fun getUserHandler(ctx: Context) {
    val userId = ctx.pathParam<Int>("id").get()
    val user = UserRepository.getUser(userId)
    if (user == null) {
        ctx.status(404)
    } else {
        ctx.json(user)
    }
}

val addUserDocs = document()
        .body<User>()
        .result<Unit>("400")
        .result<Unit>("204")

fun addUserHandler(ctx: Context) {
    val user = ctx.body<User>()
    UserRepository.addUser(user)
    ctx.status(204)
}


fun main() {
    val app = Javalin.create {
        val openApiOptions = OpenApiOptions(Info().version("1.0").description("My Application"))
                .path("/swagger-docs")
                .swagger(SwaggerOptions("/swagger").title("My Swagger Documentation"))
                .reDoc(ReDocOptions("/redoc").title("My ReDoc Documentation"))
                .defaultOperation { operation, _ ->
                    operation.responses {
                        response("500") { description = "Server Error" }
                    }
                }
        it.registerPlugin(OpenApiPlugin(openApiOptions))
    }

    with(app) {
        get("/users", documented(getUsersDocs, ::getUsersHandler))
        get("/users/:id", documented(getUserDocs, ::getUserHandler))
        post("/users", documented(addUserDocs, ::addUserHandler))
    }

    app.start()
}
