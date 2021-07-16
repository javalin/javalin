package io.javalin.plugin.openapi.dsl

import io.javalin.http.CrudHandler

class DocumentedCrudHandler(
        val crudHandlerDocumentation: OpenApiCrudHandlerDocumentation,
        private val crudHandler: CrudHandler
) : CrudHandler by crudHandler
