package io.javalin.router

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.security.RouteRole

class Endpoint @JvmOverloads constructor(
    val method: HandlerType,
    val path: String,
    val roles: Set<RouteRole> = emptySet(),
    val handler: Handler
) {

    fun handle(ctx: Context): Context {
        handler.handle(ctx)
        return ctx
    }

    fun interface EndpointExecutor {
        fun execute(endpoint: Endpoint): Context
    }

    fun handle(executor: EndpointExecutor): Context {
        return executor.execute(this)
    }

}
