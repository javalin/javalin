package io.javalin.router

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.http.NotFoundResponse
import io.javalin.security.Roles
import io.javalin.security.RouteRole

/**
 * Marker interface for endpoint metadata.
 * Requiring this interfaces prevents the user from passing existing classes (such as String) as metadata.
 */
interface EndpointMetadata

/**
 * Metadata for custom HTTP methods (e.g., WebDAV methods like PROPFIND, MKCOL).
 * This is used internally to store the custom method name for endpoints.
 */
data class CustomHttpMethod(val methodName: String) : EndpointMetadata

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

    @Deprecated("Use Endpoint builder instead", ReplaceWith("Endpoint.create(method, path)"))
    constructor(
        method: HandlerType,
        path: String,
        vararg roles: RouteRole,
        handler: Handler
    ) : this(
        method = method,
        path = path,
        metadata = setOf(Roles(roles.toSet())),
        handler = handler
    )

    @Deprecated("Use metadata instead", ReplaceWith("getMetadata(Roles.class)"))
    val roles: Set<RouteRole>
        get() = metadata(Roles::class.java)?.roles ?: emptySet()

    private val metadata = metadata.associateBy { it::class.java }

    /** Execute the endpoint handler with the given context */
    fun handle(ctx: Context): Context =
        ctx.also { handler.handle(ctx) }

    /** Execute the endpoint handler with the given executor */
    fun handle(executor: EndpointExecutor): Context =
        executor.execute(this)

    @Suppress("UNCHECKED_CAST")
    fun <METADATA : EndpointMetadata> metadata(key: Class<METADATA>): METADATA? =
        metadata[key] as METADATA?

    companion object {

        class EndpointBuilder internal constructor(val method: HandlerType, val path: String) {

            private val metadata = mutableSetOf<EndpointMetadata>()

            fun addMetadata(metadata: EndpointMetadata): EndpointBuilder =
                apply { this.metadata.add(metadata) }

            fun handler(handler: Handler): Endpoint =
                Endpoint(method, path, metadata, handler)

        }

        @Deprecated("Experimental feature")
        @JvmStatic
        fun create(method: HandlerType, path: String): EndpointBuilder = EndpointBuilder(method, path)
    }

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
