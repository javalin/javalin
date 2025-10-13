/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.router.JavalinDefaultRouting

/**
 * The possible Handler types one can use in Javalin.
 * This includes all standard HTTP methods (e.g.: GET, POST, …),
 * as well as Javalin specific operations.
 * 
 * Note: HTTP methods are now represented as strings. Standard constants are provided
 * for convenience (e.g., HandlerType.GET returns "GET"), but any HTTP method string
 * can be used, including custom methods like "PROPFIND" for WebDAV.
 */
class HandlerType private constructor(val value: String, val isHttpMethod: Boolean = true) {

    override fun toString() = value
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HandlerType) return false
        return value == other.value
    }
    
    override fun hashCode() = value.hashCode()

    companion object {
        /** The HTTP GET method requests a representation of the specified resource. */
        @JvmField val GET = HandlerType("GET")
        
        /** The HTTP POST method sends data to the server. The type of the body of the request is indicated by the Content-Type header.*/
        @JvmField val POST = HandlerType("POST")
        
        /**
         * The HTTP PUT request method creates a new resource or replaces a representation
         * of the target resource with the request payload.
         *
         * The difference between PUT and POST is that PUT is idempotent: calling it once
         * or several times successively has the same effect (that is no side effect), whereas
         * successive identical POST requests may have additional effects, akin to placing
         * an order several times.
         */
        @JvmField val PUT = HandlerType("PUT")
        
        /**
         * The HTTP PATCH request method applies partial modifications to a resource.
         *
         * PATCH is somewhat analogous to the "update" concept found in CRUD (in general,
         * HTTP is different than CRUD, and the two should not be confused).
         */
        @JvmField val PATCH = HandlerType("PATCH")
        
        /** The HTTP DELETE request method deletes the specified resource. */
        @JvmField val DELETE = HandlerType("DELETE")
        
        /** The HTTP HEAD method requests the headers that would be returned if  the HEAD request's URL was instead requested with the HTTP GET method.*/
        @JvmField val HEAD = HandlerType("HEAD")
        
        /** The HTTP TRACE method performs a message loop-back test along the path to the target resource, providing a useful debugging mechanism.*/
        @JvmField val TRACE = HandlerType("TRACE")
        
        /** The HTTP CONNECT method starts two-way communications with the requested resource. It can be used to open a tunnel. */
        @JvmField val CONNECT = HandlerType("CONNECT")
        
        /**
         * The HTTP OPTIONS method requests permitted communication options for a given URL
         * or server. A client can specify a URL with this method, or an asterisk (*) to
         * refer to the entire server.
         */
        @JvmField val OPTIONS = HandlerType("OPTIONS")
        
        /**
         * Javalin Specific: Before-handlers are matched before every request (including static files).
         * @see [JavalinDefaultRouting.before]
         */
        @JvmField val BEFORE = HandlerType("BEFORE", isHttpMethod = false)
        
        /**
         * Javalin specific: BeforeMatched-handlers run before a request finds a matching handler
         * @see [JavalinDefaultRouting.beforeMatched]
         */
        @JvmField val BEFORE_MATCHED = HandlerType("BEFORE_MATCHED", isHttpMethod = false)
        
        /**
         * Javalin specific: AfterMatched-handlers run after a request which found a matching handler
         * @see [JavalinDefaultRouting.afterMatched]
         */
        @JvmField val AFTER_MATCHED = HandlerType("AFTER_MATCHED", isHttpMethod = false)
        
        /**
         * Javalin specific: handler ran before an http request is upgraded to websocket
         * @see [JavalinDefaultRouting.wsBeforeUpgrade]
         */
        @JvmField val WEBSOCKET_BEFORE_UPGRADE = HandlerType("WEBSOCKET_BEFORE_UPGRADE", isHttpMethod = false)
        
        /**
         * Javalin specific: handler ran after an http request is upgraded to websocket
         * @see [JavalinDefaultRouting.wsAfterUpgrade]
         */
        @JvmField val WEBSOCKET_AFTER_UPGRADE = HandlerType("WEBSOCKET_AFTER_UPGRADE", isHttpMethod = false)
        
        /**
         * Javalin specific: After-handlers run after every request (even if an exception occurred)
         * @see [JavalinDefaultRouting.after]
         */
        @JvmField val AFTER = HandlerType("AFTER", isHttpMethod = false)

        private val knownTypes = listOf(GET, POST, PUT, PATCH, DELETE, HEAD, TRACE, CONNECT, OPTIONS,
            BEFORE, BEFORE_MATCHED, AFTER_MATCHED, WEBSOCKET_BEFORE_UPGRADE, WEBSOCKET_AFTER_UPGRADE, AFTER)
        private val typeMap = knownTypes.associateBy { it.value }

        /**
         * Creates a HandlerType for the given method name. Returns a known constant if available,
         * otherwise creates a new HTTP method HandlerType.
         * 
         * @param name the HTTP method name (e.g., "GET", "POST", "PROPFIND")
         * @return a HandlerType for the given method
         */
        @JvmStatic
        fun findByName(name: String): HandlerType = typeMap[name.uppercase()] ?: HandlerType(name.uppercase())
        
        /**
         * Returns all known handler types (standard HTTP methods + lifecycle handlers).
         * Custom HTTP methods created via findByName() are not included in this list.
         * 
         * @return list of all known handler types
         */
        @JvmStatic
        fun values(): List<HandlerType> = knownTypes
    }
}
