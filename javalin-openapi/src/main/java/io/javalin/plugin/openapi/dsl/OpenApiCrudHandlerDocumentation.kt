package io.javalin.plugin.openapi.dsl

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
}
