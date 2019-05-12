package io.javalin.plugin.openapi

import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.plugin.openapi.dsl.OpenApiDocumentation
import io.javalin.plugin.openapi.ui.ReDocOptions
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.info.Info

class OpenApiOptions constructor(
        val createBaseConfiguration: CreateBaseConfiguration
) {
    /** If not null, creates a GET route to get the schema as a json */
    var path: String? = null
    var roles: Set<Role> = setOf()
    /**
     * If not null, creates a GET route to the swagger ui
     * @see <a href="https://swagger.io/tools/swagger-ui/">https://swagger.io/tools/swagger-ui/</a>
     */
    var swagger: SwaggerOptions? = null
    /**
     * If not null, creates a GET route to the reDoc ui
     * @see <a href="https://github.com/Rebilly/ReDoc">https://github.com/Rebilly/ReDoc</a>
     */
    var reDoc: ReDocOptions? = null
    /**
     * Function that is applied to every new operation.
     * You can use this to set defaults (like a 500 response).
     */
    var defaultOperation: ApplyDefaultOperation? = null

    constructor(createBaseConfiguration: () -> OpenAPI) : this(CreateBaseConfiguration(createBaseConfiguration))
    constructor(info: Info) : this({ OpenAPI().info(info) })

    fun path(value: String) = apply { path = value }
    fun swagger(value: SwaggerOptions) = apply { swagger = value }
    fun reDoc(value: ReDocOptions) = apply { reDoc = value }
    fun roles(value: Set<Role>) = apply { roles = value }
    fun defaultOperation(value: ApplyDefaultOperation) = apply { defaultOperation = value }
    fun defaultOperation(setup: (operation: Operation, documentation: OpenApiDocumentation?) -> Unit) = apply {
        defaultOperation = object : ApplyDefaultOperation {
            override fun setup(operation: Operation, documentation: OpenApiDocumentation?) = setup(operation, documentation)
        }
    }

    fun getFullDocumentationUrl(ctx: Context) = "${ctx.contextPath()}${path!!}"
}

@FunctionalInterface
interface ApplyDefaultOperation {
    fun setup(operation: Operation, documentation: OpenApiDocumentation?)
}
