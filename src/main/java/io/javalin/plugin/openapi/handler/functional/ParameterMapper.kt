package io.javalin.plugin.openapi.handler.functional

import io.javalin.http.Context

@FunctionalInterface
interface ParameterMapper<T : SerializableFunction> {
    @Throws(Exception::class)
    fun map(ctx: Context, mapper: T)
}
