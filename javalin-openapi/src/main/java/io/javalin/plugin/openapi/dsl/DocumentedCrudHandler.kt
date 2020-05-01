package io.javalin.plugin.openapi.dsl

import io.javalin.apibuilder.CrudHandler
import io.javalin.http.Context

class DocumentedCrudHandler(
        val crudHandlerDocumentation: OpenApiCrudHandlerDocumentation,
        private val crudHandler: CrudHandler
) : CrudHandler by crudHandler
