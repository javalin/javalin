/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.router

import java.util.*

/**
 * A read-only collection of endpoints visited during a request.
 *
 * This class provides access to the endpoint stack, which contains all endpoints
 * that have been matched and executed during the current request lifecycle
 * (BEFORE handlers, HTTP endpoint, AFTER handlers).
 */
class Endpoints internal constructor() {

    private val stack: LinkedList<Endpoint> = LinkedList()

    /** Get the last endpoint in the stack */
    fun current(): Endpoint = stack.last()

    /**
     * Get the last HTTP endpoint (GET, POST, PUT, DELETE, etc.) in the stack.
     * This is useful for finding the matched HTTP handler in AFTER handlers.
     *
     * @return The last HTTP endpoint, or null if no HTTP endpoint has been matched yet
     */
    fun lastHttpEndpoint(): Endpoint? = stack.findLast { it.method.isHttpMethod }

    /**
     * Get all endpoints as an unmodifiable list.
     * This is useful when you need to pass the endpoints to APIs that expect a List.
     */
    fun list(): List<Endpoint> = Collections.unmodifiableList(stack)

    // ---------------------------
    // PROJECT INTERNAL FUNCTIONS
    // ---------------------------

    /**
     * Add an endpoint to the stack.
     * This method is internal and should not be visible to API users.
     */
    @JvmSynthetic
    internal fun add(endpoint: Endpoint) {
        stack.add(endpoint)
        val params = endpoint.metadata(PathParams::class.java)?.params
        if (params?.isNotEmpty() == true) {
            lastEndpointWithPathParams = endpoint to params
        }
    }

    /**
     * Store the last endpoint with path parameters and its extracted params.
     * This is used internally to support path param access in handlers that don't have path params themselves
     * (e.g., AFTER handlers can access path params from the matched HTTP endpoint).
     */
    @JvmSynthetic
    internal var lastEndpointWithPathParams: Pair<Endpoint?, Map<String, String>> = Pair(null, emptyMap())

}

