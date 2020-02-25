package io.javalin.plugin.openapi.handler

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.openapi.handler.functional.ParameterMapper
import io.javalin.plugin.openapi.handler.functional.SerializableFunction

class ParameterHandler<T : SerializableFunction>(
        val reference: T,
        val mapper: ParameterMapper<T>
) : Handler {
    @Throws(Exception::class)
    override fun handle(ctx: Context) {
        mapper.map(ctx, reference)
    }
}
