package io.javalin.router

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.http.NotFoundResponse

/**
 * Marker interface for endpoint metadata.
 * Requiring this interfaces prevents the user from passing existing classes (such as String) as metadata.
 */
interface EndpointMetadata

/**
 * Metadata for storing path parameters extracted from the request URI.
 */
data class PathParams(val params: Map<String, String>) : EndpointMetadata

/**
 * Represents an HTTP endpoint in the application.
 *
 * @param method The HTTP method of the endpoint
 * @param path The path of the endpoint
 * @param metadata The metadata of the endpoint
 * @param handler The handler of the endpoint
 */
open class Endpoint @JvmOverloads constructor(
    @JvmField val method: HandlerType,
    @JvmField val path: String,
    metadata: Set<EndpointMetadata> = emptySet(),
    val handler: Handler
) {

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

    /**
     * Creates a copy of this endpoint with additional metadata.
     */
    fun withMetadata(newMetadata: EndpointMetadata): Endpoint =
        Endpoint(method, path, metadata.values.toSet() + newMetadata, handler)

    companion object {

        class EndpointBuilder internal constructor(val method: HandlerType, val path: String) {

            private val metadata = mutableSetOf<EndpointMetadata>()

            fun addMetadata(metadata: EndpointMetadata): EndpointBuilder =
                apply { this.metadata.add(metadata) }

            fun handler(handler: Handler): Endpoint =
                Endpoint(method, path, metadata, handler)

        }

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
) : NotFoundResponse("Endpoint ${method.name()} $path not found")
