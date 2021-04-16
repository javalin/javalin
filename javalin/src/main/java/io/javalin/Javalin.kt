/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */
package io.javalin

import io.javalin.apibuilder.ApiBuilder
import io.javalin.apibuilder.EndpointGroup
import io.javalin.core.JavalinConfig
import io.javalin.core.JavalinConfig.Companion.applyUserConfig
import io.javalin.core.JavalinServer
import io.javalin.core.JettyUtil.disableJettyLogger
import io.javalin.core.event.EventListener
import io.javalin.core.event.EventManager
import io.javalin.core.event.HandlerMetaInfo
import io.javalin.core.event.JavalinEvent
import io.javalin.core.event.WsHandlerMetaInfo
import io.javalin.core.security.Role
import io.javalin.core.util.Util.isNonSubPathWildcard
import io.javalin.core.util.Util.logIfServerNotStarted
import io.javalin.core.util.Util.logJavalinBanner
import io.javalin.core.util.Util.prefixContextPath
import io.javalin.core.util.Util.printHelpfulMessageIfLoggerIsMissing
import io.javalin.http.ExceptionHandler
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.http.JavalinServlet
import io.javalin.http.contentTypeWrap
import io.javalin.http.sse.SseClient
import io.javalin.http.sse.SseHandler
import io.javalin.websocket.JavalinWsServlet
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsExceptionHandler
import io.javalin.websocket.WsHandlerType
import org.slf4j.LoggerFactory
import java.util.function.Consumer

open class Javalin {
    /**
     * Do not use this field unless you know what you're doing.
     * Application config should be declared in [Javalin.create]
     */
    @JvmField
    var config = JavalinConfig()
    internal var server: JavalinServer? // null in standalone-mode
    internal var wsServlet: JavalinWsServlet? // null in standalone-mode
    internal var servlet = JavalinServlet(config)
    internal var eventManager = EventManager()

    constructor() {
        server = JavalinServer(config)
        wsServlet = JavalinWsServlet(config, servlet)
    }

    constructor(server: JavalinServer?, wsServlet: JavalinWsServlet?) {
        this.server = server
        this.wsServlet = wsServlet
    }

    // Get JavalinServlet (for use in standalone mode)
    fun servlet(): JavalinServlet {
        return servlet
    }

    fun wsServlet(): JavalinWsServlet? {
        return wsServlet
    }

    /**
     * Get the JavalinServer
     */
    fun server(): JavalinServer? {
        return server
    }

    /**
     * Synchronously starts the application instance on the specified port
     * with the given host IP to bind to.
     *
     * @param host The host IP to bind to
     * @param port to run on
     * @return running application instance.
     * @see Javalin.create
     * @see Javalin.start
     */
    fun start(host: String, port: Int): Javalin {
        server!!.serverHost = host
        return start(port)
    }

    /**
     * Synchronously starts the application instance on the specified port.
     *
     * @param port to run on
     * @return running application instance.
     * @see Javalin.create
     * @see Javalin.start
     */
    fun start(port: Int): Javalin {
        server!!.serverPort = port
        return start()
    }

    /**
     * Synchronously starts the application instance on the default port (7000).
     * To start on a random port use [Javalin.start] with port 0.
     *
     * @return running application instance.
     * @see Javalin.create
     */
    fun start(): Javalin {
        logJavalinBanner(config.showJavalinBanner)
        disableJettyLogger()
        val startupTimer = System.currentTimeMillis()
        if (server!!.started) {
            val message = "Server already started. If you are trying to call start() on an instance " +
                    "of Javalin that was stopped using stop(), please create a new instance instead."
            throw IllegalStateException(message)
        }
        server!!.started = true
        printHelpfulMessageIfLoggerIsMissing()
        eventManager.fireEvent(JavalinEvent.SERVER_STARTING)
        try {
            log.info("Starting Javalin ...")
            server!!.start(wsServlet!!)
            log.info("Javalin started in " + (System.currentTimeMillis() - startupTimer) + "ms \\o/")
            eventManager.fireEvent(JavalinEvent.SERVER_STARTED)
        } catch (e: Exception) {
            log.error("Failed to start Javalin")
            eventManager.fireEvent(JavalinEvent.SERVER_START_FAILED)
            if (server!!.server().getAttribute("is-default-server") == true) {
                stop() // stop if server is default server; otherwise, the caller is responsible to stop
            }
            if (e.message != null && e.message!!.contains("Failed to bind to")) {
                throw RuntimeException("Port already in use. Make sure no other process is using port " + server!!.serverPort + " and try again.", e)
            } else if (e.message != null && e.message!!.contains("Permission denied")) {
                throw RuntimeException("Port 1-1023 require elevated privileges (process must be started by admin).", e)
            }
            throw RuntimeException(e)
        }
        return this
    }

    /**
     * Synchronously stops the application instance.
     *
     * @return stopped application instance.
     */
    fun stop(): Javalin {
        log.info("Stopping Javalin ...")
        eventManager.fireEvent(JavalinEvent.SERVER_STOPPING)
        try {
            server!!.server().stop()
        } catch (e: Exception) {
            log.error("Javalin failed to stop gracefully", e)
        }
        log.info("Javalin has stopped")
        eventManager.fireEvent(JavalinEvent.SERVER_STOPPED)
        return this
    }

    fun events(listener: Consumer<EventListener>): Javalin {
        val eventListener = EventListener(eventManager)
        listener.accept(eventListener)
        return this
    }

    /**
     * Get which port instance is running on
     * Mostly useful if you start the instance with port(0) (random port)
     */
    fun port(): Int {
        return server!!.serverPort
    }

    /**
     * Registers an attribute on the instance.
     * Instance is available on the [Context] through [Context.appAttribute].
     * Ex: app.attribute(MyExt.class, myExtInstance())
     * The method must be called before [Javalin.start].
     */
    fun <T> attribute(clazz: Class<T>, obj: Any?): Javalin {
        config.inner.appAttributes[clazz] = obj!!
        return this
    }

    /**
     * Retrieve an attribute stored on the instance.
     * Available on the [Context] through [Context.appAttribute].
     * Ex: app.attribute(MyExt.class).myMethod()
     * Ex: ctx.appAttribute(MyExt.class).myMethod()
     */
    fun <T> attribute(clazz: Class<T>): T? {
        return config.inner.appAttributes[clazz] as T?
    }

    /**
     * Creates a temporary static instance in the scope of the endpointGroup.
     * Allows you to call get(handler), post(handler), etc. without without using the instance prefix.
     *
     * @see [Handler groups in documentation](https://javalin.io/documentation.handler-groups)
     *
     * @see ApiBuilder
     */
    fun routes(endpointGroup: EndpointGroup): Javalin {
        ApiBuilder.setStaticJavalin(this)
        try {
            endpointGroup.addEndpoints()
        } finally {
            ApiBuilder.clearStaticJavalin()
        }
        return this
    }
    // ********************************************************************************************
    // HTTP
    // ********************************************************************************************
    /**
     * Adds an exception mapper to the instance.
     *
     * @see [Exception mapping in docs](https://javalin.io/documentation.exception-mapping)
     */
    fun <T : Exception> exception(exceptionClass: Class<T>, exceptionHandler: ExceptionHandler<in T>): Javalin {
        servlet.exceptionMapper.handlers[exceptionClass] = exceptionHandler as ExceptionHandler<Exception>
        return this
    }

    /**
     * Adds an error mapper to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     *
     * @see [Error mapping in docs](https://javalin.io/documentation.error-mapping)
     */
    fun error(statusCode: Int, handler: Handler): Javalin {
        servlet.errorMapper.errorHandlerMap[statusCode] = handler
        return this
    }

    /**
     * Adds an error mapper for the specified content-type to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     *
     * @see [Error mapping in docs](https://javalin.io/documentation.error-mapping)
     */
    fun error(statusCode: Int, contentType: String, handler: Handler): Javalin {
        return error(statusCode, contentTypeWrap(contentType, handler))
    }

    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * Requires an access manager to be set on the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     *
     * @see AccessManager
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun addHandler(handlerType: HandlerType, path: String, handler: Handler, roles: Set<Role>): Javalin {
        if (isNonSubPathWildcard(path)) { // TODO: This should probably be made part of the actual path matching
            // split into two handlers: one exact, and one sub-path with wildcard
            val basePath = path.substring(0, path.length - 1)
            addHandler(handlerType, basePath, handler, roles)
            return addHandler(handlerType, "$basePath/*", handler, roles)
        }
        servlet.addHandler(handlerType, path, handler, roles)
        eventManager.fireHandlerAddedEvent(HandlerMetaInfo(handlerType, prefixContextPath(servlet.config.contextPath, path), handler, roles))
        return this
    }

    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun addHandler(httpMethod: HandlerType, path: String, handler: Handler): Javalin {
        return addHandler(httpMethod, path, handler, HashSet()) // no roles set for this route (open to everyone with default access manager)
    }

    /**
     * Adds a GET request handler for the specified path to the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    operator fun get(path: String, handler: Handler): Javalin {
        return addHandler(HandlerType.GET, path, handler)
    }

    /**
     * Adds a POST request handler for the specified path to the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun post(path: String, handler: Handler): Javalin {
        return addHandler(HandlerType.POST, path, handler)
    }

    /**
     * Adds a PUT request handler for the specified path to the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun put(path: String, handler: Handler): Javalin {
        return addHandler(HandlerType.PUT, path, handler)
    }

    /**
     * Adds a PATCH request handler for the specified path to the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun patch(path: String, handler: Handler): Javalin {
        return addHandler(HandlerType.PATCH, path, handler)
    }

    /**
     * Adds a DELETE request handler for the specified path to the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun delete(path: String, handler: Handler): Javalin {
        return addHandler(HandlerType.DELETE, path, handler)
    }

    /**
     * Adds a HEAD request handler for the specified path to the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun head(path: String, handler: Handler): Javalin {
        return addHandler(HandlerType.HEAD, path, handler)
    }

    /**
     * Adds a OPTIONS request handler for the specified path to the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun options(path: String, handler: Handler): Javalin {
        return addHandler(HandlerType.OPTIONS, path, handler)
    }
    // ********************************************************************************************
    // Secured HTTP verbs
    // ********************************************************************************************
    /**
     * Adds a GET request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    operator fun get(path: String, handler: Handler, permittedRoles: Set<Role>): Javalin {
        return addHandler(HandlerType.GET, path, handler, permittedRoles)
    }

    /**
     * Adds a POST request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun post(path: String, handler: Handler, permittedRoles: Set<Role>): Javalin {
        return addHandler(HandlerType.POST, path, handler, permittedRoles)
    }

    /**
     * Adds a PUT request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun put(path: String, handler: Handler, permittedRoles: Set<Role>): Javalin {
        return addHandler(HandlerType.PUT, path, handler, permittedRoles)
    }

    /**
     * Adds a PATCH request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun patch(path: String, handler: Handler, permittedRoles: Set<Role>): Javalin {
        return addHandler(HandlerType.PATCH, path, handler, permittedRoles)
    }

    /**
     * Adds a DELETE request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun delete(path: String, handler: Handler, permittedRoles: Set<Role>): Javalin {
        return addHandler(HandlerType.DELETE, path, handler, permittedRoles)
    }

    /**
     * Adds a HEAD request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun head(path: String, handler: Handler, permittedRoles: Set<Role>): Javalin {
        return addHandler(HandlerType.HEAD, path, handler, permittedRoles)
    }

    /**
     * Adds a OPTIONS request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun options(path: String, handler: Handler, permittedRoles: Set<Role>): Javalin {
        return addHandler(HandlerType.OPTIONS, path, handler, permittedRoles)
    }
    // ********************************************************************************************
    // Server-sent events
    // ********************************************************************************************
    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     */
    fun sse(path: String, client: Consumer<SseClient>): Javalin {
        return get(path, SseHandler(client), setOf())
    }

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     * Requires an access manager to be set on the instance.
     */
    fun sse(path: String, client: Consumer<SseClient>, permittedRoles: Set<Role>): Javalin {
        return get(path, SseHandler(client), permittedRoles)
    }
    // ********************************************************************************************
    // Before/after handlers (filters)
    // ********************************************************************************************
    /**
     * Adds a BEFORE request handler for the specified path to the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.before-handlers)
     */
    fun before(path: String, handler: Handler): Javalin {
        return addHandler(HandlerType.BEFORE, path, handler)
    }

    /**
     * Adds a BEFORE request handler for all routes in the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.before-handlers)
     */
    fun before(handler: Handler): Javalin {
        return before("*", handler)
    }

    /**
     * Adds an AFTER request handler for the specified path to the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.before-handlers)
     */
    fun after(path: String, handler: Handler): Javalin {
        return addHandler(HandlerType.AFTER, path, handler)
    }

    /**
     * Adds an AFTER request handler for all routes in the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.before-handlers)
     */
    fun after(handler: Handler): Javalin {
        return after("*", handler)
    }
    // ********************************************************************************************
    // WebSocket
    // ********************************************************************************************
    /**
     * Adds a WebSocket exception mapper to the instance.
     *
     * @see [Exception mapping in docs](https://javalin.io/documentation.exception-mapping)
     */
    fun <T : Exception> wsException(exceptionClass: Class<T>, exceptionHandler: WsExceptionHandler<in T>): Javalin {
        wsServlet!!.wsExceptionMapper.handlers[exceptionClass] = exceptionHandler as WsExceptionHandler<Exception>
        return this
    }
    /**
     * Adds a specific WebSocket handler for the given path to the instance.
     * Requires an access manager to be set on the instance.
     */
    /**
     * Adds a specific WebSocket handler for the given path to the instance.
     */
    private fun addWsHandler(handlerType: WsHandlerType, path: String, wsConfig: Consumer<WsConfig>, roles: Set<Role> = HashSet()): Javalin {
        wsServlet!!.addHandler(handlerType, path, wsConfig, roles)
        eventManager.fireWsHandlerAddedEvent(WsHandlerMetaInfo(handlerType, prefixContextPath(servlet.config.contextPath, path), wsConfig, roles))
        return this
    }

    /**
     * Adds a WebSocket handler on the specified path.
     *
     * @see [WebSockets in docs](https://javalin.io/documentation.websockets)
     */
    fun ws(path: String, ws: Consumer<WsConfig>): Javalin {
        return addWsHandler(WsHandlerType.WEBSOCKET, path, ws, setOf())
    }

    /**
     * Adds a WebSocket handler on the specified path with the specified roles.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see [WebSockets in docs](https://javalin.io/documentation.websockets)
     */
    fun ws(path: String, ws: Consumer<WsConfig>, permittedRoles: Set<Role> = setOf()): Javalin {
        return addWsHandler(WsHandlerType.WEBSOCKET, path, ws, permittedRoles)
    }

    /**
     * Adds a WebSocket before handler for the specified path to the instance.
     */
    fun wsBefore(path: String, wsConfig: Consumer<WsConfig>): Javalin {
        return addWsHandler(WsHandlerType.WS_BEFORE, path, wsConfig)
    }

    /**
     * Adds a WebSocket before handler for all routes in the instance.
     */
    fun wsBefore(wsConfig: Consumer<WsConfig>): Javalin {
        return wsBefore("*", wsConfig)
    }

    /**
     * Adds a WebSocket after handler for the specified path to the instance.
     */
    fun wsAfter(path: String, wsConfig: Consumer<WsConfig>): Javalin {
        return addWsHandler(WsHandlerType.WS_AFTER, path, wsConfig)
    }

    /**
     * Adds a WebSocket after handler for all routes in the instance.
     */
    fun wsAfter(wsConfig: Consumer<WsConfig>): Javalin {
        return wsAfter("*", wsConfig)
    }

    companion object Factory {

        @JvmField
        var log = LoggerFactory.getLogger(Javalin::class.java)

        @JvmStatic fun create() = create({})
        @JvmStatic fun create(config: Consumer<JavalinConfig>): Javalin {
            val app = Javalin()
            applyUserConfig(app, app.config, config) // mutates app.config and app (adds http-handlers)
            if (app.config.logIfServerNotStarted) {
                logIfServerNotStarted(app.server!!)
            }
            return app
        }


        @JvmStatic fun createStandalone() = createStandalone({})
        @JvmStatic fun createStandalone(config: Consumer<JavalinConfig>): Javalin {
            val app = Javalin(null, null)
            applyUserConfig(app, app.config, config) // mutates app.config and app (adds http-handlers)
            return app
        }

    }
}
