package io.javalin.plugin.openapi.dsl

import io.javalin.apibuilder.CrudHandler

class DocumentedCrudHandler(
        val crudHandlerDocumentation: OpenApiCrudHandlerDocumentation,
        private val crudHandler: CrudHandler
) : CrudHandler by crudHandler
