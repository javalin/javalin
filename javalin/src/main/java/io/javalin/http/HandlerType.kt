/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.router.JavalinDefaultRouting

/**
 * The possible Handler types one can use in Javalin.
 * This includes all standard HTTM methods (e.g.: GET, POST, …),
 * as well as Javalin specific operations.
 * @param isHttpMethod whether the handler is a standard HTTP method.
 */
enum class HandlerType(val isHttpMethod: Boolean = true) {

    /** The HTTP GET method requests a representation of the specified resource. */
    GET,

    /** The HTTP POST method sends data to the server. The type of the body of the request is indicated by the Content-Type header.*/
    POST,

    /**
     * The HTTP PUT request method creates a new resource or replaces a representation
     * of the target resource with the request payload.
     *
     * The difference between PUT and POST is that PUT is idempotent: calling it once
     * or several times successively has the same effect (that is no side effect), whereas
     * successive identical POST requests may have additional effects, akin to placing
     * an order several times.
     */
    PUT,

    /**
     * The HTTP PATCH request method applies partial modifications to a resource.
     *
     * PATCH is somewhat analogous to the "update" concept found in CRUD (in general,
     * HTTP is different than CRUD, and the two should not be confused).
     */
    PATCH,

    /** The HTTP DELETE request method deletes the specified resource. */
    DELETE,

    /** The HTTP HEAD method requests the headers that would be returned if  the HEAD request's URL was instead requested with the HTTP GET method.*/
    HEAD,

    /** The HTTP TRACE method performs a message loop-back test along the path to the target resource, providing a useful debugging mechanism.*/
    TRACE,

    /** The HTTP CONNECT method starts two-way communications with the requested resource. It can be used to open a tunnel. */
    CONNECT,

    /**
     * The HTTP OPTIONS method requests permitted communication options for a given URL
     * or server. A client can specify a URL with this method, or an asterisk (*) to
     * refer to the entire server.
     */
    OPTIONS,

    /**
     * Javalin Specific: Before-handlers are matched before every request (including static files).
     * @see [JavalinDefaultRouting.before]
     */
    BEFORE(isHttpMethod = false),

    /**
     * Javalin specific: BeforeMatched-handlers run before a request finds a matching handler
     * @see [JavalinDefaultRouting.beforeMatched]
     */
    BEFORE_MATCHED(isHttpMethod = false),

    /**
     * Javalin specific: AfterMatched-handlers run after a request which found a matching handler
     * @see [JavalinDefaultRouting.afterMatched]
     */
    AFTER_MATCHED(isHttpMethod = false),

    /**
     * Javalin specific: handler ran before an http request is upgraded to websocket
     * @see [JavalinDefaultRouting.wsBeforeUpgrade]
     */
    WEBSOCKET_BEFORE_UPGRADE(isHttpMethod = false),

    /**
     *
     * Javalin specific: handler ran after an http request is upgraded to websocket
     * @see [JavalinDefaultRouting.wsAfterUpgrade]
     */
    WEBSOCKET_AFTER_UPGRADE(isHttpMethod = false),

    /**
     * Javalin specific: After-handlers run after every request (even if an exception occurred)
     * @see [JavalinDefaultRouting.after]
     */
    AFTER(isHttpMethod = false),

    /**
     * Javalin specific: this corresponds to a handler that is not recognized using the
     * [findByName] method.
     */
    INVALID(isHttpMethod = false);

    companion object {

        private val methodMap = values().associateBy { it.toString() }
        private val customHttpMethods = mutableSetOf<String>()

        /**
         * Registers a custom HTTP method (e.g., WebDAV methods like PROPFIND, MKCOL).
         * Once registered, the method can be used with [io.javalin.router.JavalinDefaultRoutingApi.addHttpHandler].
         * 
         * Note: This should be called before starting the Javalin server.
         * 
         * @param methodName the HTTP method name (e.g., "PROPFIND", "MKCOL")
         */
        @JvmStatic
        fun registerCustomHttpMethod(methodName: String) {
            customHttpMethods.add(methodName.uppercase())
        }

        /**
         * Checks if a method name is a registered custom HTTP method.
         * 
         * @param methodName the HTTP method name to check
         * @return true if the method is a registered custom HTTP method
         */
        @JvmStatic
        fun isCustomHttpMethod(methodName: String): Boolean {
            return customHttpMethods.contains(methodName.uppercase())
        }

        fun findByName(name: String): HandlerType = methodMap[name.uppercase()] ?: INVALID

    }

}
