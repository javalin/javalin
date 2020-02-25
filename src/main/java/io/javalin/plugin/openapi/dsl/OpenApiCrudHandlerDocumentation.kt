package io.javalin.plugin.openapi.dsl

import io.javalin.apibuilder.CrudHandlerType

data class OpenApiCrudHandlerDocumentation(
        var getAllDocumentation: OpenApiDocumentation = OpenApiDocumentation(),
        var getOneDocumentation: OpenApiDocumentation = OpenApiDocumentation(),
        var createDocumentation: OpenApiDocumentation = OpenApiDocumentation(),
        var updateDocumentation: OpenApiDocumentation = OpenApiDocumentation(),
        var deleteDocumentation: OpenApiDocumentation = OpenApiDocumentation()
) {
    fun getAll(doc: OpenApiDocumentation) = apply { this.getAllDocumentation = doc }
    fun getOne(doc: OpenApiDocumentation) = apply { this.getOneDocumentation = doc }
    fun create(doc: OpenApiDocumentation) = apply { this.createDocumentation = doc }
    fun update(doc: OpenApiDocumentation) = apply { this.updateDocumentation = doc }
    fun delete(doc: OpenApiDocumentation) = apply { this.deleteDocumentation = doc }

    fun asMap() = mapOf(
            CrudHandlerType.GET_ALL to getAllDocumentation,
            CrudHandlerType.GET_ONE to getOneDocumentation,
            CrudHandlerType.CREATE to createDocumentation,
            CrudHandlerType.UPDATE to updateDocumentation,
            CrudHandlerType.DELETE to deleteDocumentation
    )
}
