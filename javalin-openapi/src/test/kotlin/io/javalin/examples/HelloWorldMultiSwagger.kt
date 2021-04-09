/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.examples

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.InternalServerErrorResponse
import io.javalin.http.context.body
import io.javalin.http.context.pathParam
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.dsl.document
import io.javalin.plugin.openapi.dsl.documented
import io.javalin.plugin.openapi.ui.ReDocOptions
import io.javalin.plugin.openapi.ui.RedocOptionsObject
import io.javalin.plugin.openapi.ui.RedocOptionsTheme
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.swagger.v3.oas.models.info.Info


data class Item(val id: Int, val name: String, val stock: Int, val price: Double)

object ItemRepository {
    private val items = mutableListOf<Item>()
    fun getItems() = items.toList()
    fun getItem(id: Int) = items.find { it.id == id }
    fun addItem(item: Item) = items.add(item)
}

val getItemsDocs = document().jsonArray<User>("200") { it.description = "Returns all items" }

fun getItemsHandler(ctx: Context) {
    val users = ItemRepository.getItems()
    ctx.json(users)
}

val getItemDocs = document()
        .pathParam<Int>("id")
        .json<Item>("200") { it.description = "Returns item with id" }
        .result<Unit>("404")

fun getItemHandler(ctx: Context) {
    val itemId = ctx.pathParam<Int>("id").get()
    val item = ItemRepository.getItem(itemId)
    if (item == null) {
        ctx.status(404)
    } else {
        ctx.json(item)
    }
}

val addItemDocs = document()
        .body<Item>()
        .result<Unit>("400")
        .result<Unit>("204")

fun addItemHandler(ctx: Context) {
    val item = ctx.body<Item>()
    ItemRepository.addItem(item)
    ctx.status(204)
}


fun main() {
    val app = Javalin.create {
        val fullOpenApiOptions = OpenApiOptions(Info().version("1.0").description("My Application"))
                .path("/swagger-docs")
                .swagger(SwaggerOptions("/swagger").title("My Swagger Documentation"))
                .reDoc(ReDocOptions("/redoc", RedocOptionsObject(
                        hideDownloadButton = true,
                        theme = RedocOptionsTheme(
                                spacingUnit = 10,
                                isTypographyOptimizeSpeed = true
                        )
                )).title("My ReDoc Documentation"))
                .defaultDocumentation { documentation -> documentation.json<InternalServerErrorResponse>("500") }
        val itemsOnly = OpenApiOptions(Info().version("1.0").description("My Application"))
                .path("/swagger-docs-items")
                .swagger(SwaggerOptions("/swagger-items").title("My Swagger Documentation -- ITEMS only"))
                .defaultDocumentation { documentation -> documentation.json<InternalServerErrorResponse>("500") }
                .ignorePath("/users*")
        val includeOnly = OpenApiOptions(Info().version("1.0").description("My Application"))
            .path("/swagger-docs-includeonly")
            .swagger(SwaggerOptions("/swagger-includeonly").title("My Swagger Documentation -- included paths only"))
            .defaultDocumentation { documentation -> documentation.json<InternalServerErrorResponse>("500") }
            .includePath("/items/*")

        it.registerPlugin(OpenApiPlugin(fullOpenApiOptions, itemsOnly, includeOnly))
    }

    with(app) {
        get("/users", documented(getUsersDocs, ::getUsersHandler))
        get("/users/:id", documented(getUserDocs, ::getUserHandler))
        post("/users", documented(addUserDocs, ::addUserHandler))
        get("/items", documented(getItemsDocs, ::getItemsHandler))
        get("/items/:id", documented(getItemDocs, ::getItemHandler))
        post("/items", documented(addItemDocs, ::addItemHandler))
    }

    app.start()
}
