package io.javalin.router

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.http.NotFoundResponse
import io.javalin.security.RouteRole

/**
 * Represents an HTTP endpoint in the application.
 *
 * @param method The HTTP method of the endpoint
 * @param path The path of the endpoint
 * @param roles The roles that are allowed to access the endpoint (optional)
 * @param handler The handler of the endpoint
 */
open class Endpoint @JvmOverloads constructor(
    val method: HandlerType,
    val path: String,
    vararg roles: RouteRole = emptyArray(),
    val handler: Handler
) {

    val roles = roles.toSet()

    /** Execute the endpoint handler with the given context */
    fun handle(ctx: Context): Context =
        ctx.also { handler.handle(ctx) }

    /** Execute the endpoint handler with the given executor */
    fun handle(executor: EndpointExecutor): Context =
        executor.execute(this)

}

/**
 * Endpoint executor represents a component that is able to execute an endpoint,
 * such as a router or a mock.
 */
fun interface EndpointExecutor {
    fun execute(endpoint: Endpoint): Context
}

class EndpointNotFound(
    method: HandlerType,
    path: String
) : NotFoundResponse("Endpoint ${method.name} $path not found")
