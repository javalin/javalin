package io.javalin.router

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.http.NotFoundResponse
import io.javalin.security.Roles
import io.javalin.security.RouteRole

/** Represents metadata object for an endpoint */
interface EndpointMetadata

/**
 * Represents an HTTP endpoint in the application.
 *
 * @param method The HTTP method of the endpoint
 * @param path The path of the endpoint
 * @param metadata The metadata of the endpoint
 * @param handler The handler of the endpoint
 */
open class Endpoint @JvmOverloads constructor(
    val method: HandlerType,
    val path: String,
    metadata: Set<EndpointMetadata> = emptySet(),
    val handler: Handler
) {

    private val metadata = metadata.associateBy { it::class.java }

    constructor(
        method: HandlerType,
        path: String,
        vararg roles: RouteRole,
        handler: Handler
    ) : this(
        method = method,
        path = path,
        metadata = setOf(Roles(*roles)),
        handler = handler
    )

    companion object {

        class EndpointBuilder internal constructor(val method: HandlerType, val path: String) {

            private val metadata = mutableSetOf<EndpointMetadata>()

            fun addMetadata(vararg metadata: EndpointMetadata): EndpointBuilder =
                apply { this.metadata.addAll(metadata) }

            fun handler(handler: Handler): Endpoint =
                Endpoint(method, path, metadata, handler)

        }

        @JvmStatic
        fun create(method: HandlerType, path: String): EndpointBuilder = EndpointBuilder(method, path)
    }


    @Deprecated("Use getMetadata instead", ReplaceWith("getMetadata(key)"))
    val roles: Set<RouteRole>
        get() = metadata() ?: emptySet()

    /** Execute the endpoint handler with the given context */
    fun handle(ctx: Context): Context =
        ctx.also { handler.handle(ctx) }

    /** Execute the endpoint handler with the given executor */
    fun handle(executor: EndpointExecutor): Context =
        executor.execute(this)

    @Suppress("UNCHECKED_CAST")
    fun <M : EndpointMetadata> metadata(key: Class<M>): M? =
        metadata[key] as M?

    inline fun <reified M : EndpointMetadata> metadata(): M? =
        metadata(M::class.java)

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
