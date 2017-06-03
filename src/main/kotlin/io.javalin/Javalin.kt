/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.ErrorMapper
import io.javalin.core.ExceptionMapper
import io.javalin.core.PathMatcher
import io.javalin.core.util.Util
import io.javalin.embeddedserver.EmbeddedServer
import io.javalin.embeddedserver.EmbeddedServerFactory
import io.javalin.embeddedserver.jetty.EmbeddedJettyFactory
import io.javalin.lifecycle.Event
import io.javalin.lifecycle.EventListener
import io.javalin.lifecycle.EventManager
import io.javalin.security.AccessManager
import io.javalin.security.Role
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch

class Javalin {

    private val log = LoggerFactory.getLogger(Javalin::class.java)

    companion object {
        var DEFAULT_PORT = 7000
        @JvmStatic fun create(): Javalin {
            return Javalin()
        }
    }

    private var port = DEFAULT_PORT

    private var ipAddress = "0.0.0.0"

    private var embeddedServer: EmbeddedServer? = null
    private var embeddedServerFactory: EmbeddedServerFactory = EmbeddedJettyFactory()

    private var staticFileDirectory: String? = null
    private var pathMatcher = PathMatcher()
    private var exceptionMapper = ExceptionMapper()
    private var errorMapper = ErrorMapper()

    fun clearInternalMappers() {
        pathMatcher.clear();
        exceptionMapper.clear();
        errorMapper.clear();
    }

    private val eventManager = EventManager()

    private var startLatch = CountDownLatch(1)
    private var stopLatch = CountDownLatch(1)

    private var accessManager = AccessManager { handler: Handler, req: Request, res: Response, roles: List<Role> ->
        throw IllegalStateException("No access manager configured. Add an access manager using 'accessManager()'")
    }

    fun accessManager(accessManager: AccessManager): Javalin {
        this.accessManager = accessManager
        return this
    }

    private var started = false

    @Synchronized fun start(): Javalin {
        if (!started) {
            log.info(Util.javalinBanner())
            Util.printHelpfulMessageIfLoggerIsMissing()
            Thread {
                eventManager.fireEvent(Event.Type.SERVER_STARTING, this)
                try {
                    embeddedServer = embeddedServerFactory.create(pathMatcher, exceptionMapper, errorMapper, staticFileDirectory)
                    port = embeddedServer!!.start(ipAddress, port)
                } catch (e: Exception) {
                    log.error("Failed to start Javalin")
                    eventManager.fireEvent(Event.Type.SERVER_START_FAILED, this)
                }

                eventManager.fireEvent(Event.Type.SERVER_STARTED, this)
                try {
                    startLatch.countDown()
                    embeddedServer!!.join()
                } catch (e: InterruptedException) {
                    log.error("Server startup interrupted", e)
                    Thread.currentThread().interrupt()
                }
            }.start()
            started = true
        }
        return this
    }

    fun awaitInitialization(): Javalin {
        if (!started) {
            throw IllegalStateException("Server hasn't been started. Call start() before calling this method.")
        }
        try {
            startLatch.await()
        } catch (e: InterruptedException) {
            log.info("awaitInitialization was interrupted")
            Thread.currentThread().interrupt()
        }

        return this
    }

    @Synchronized fun stop(): Javalin {
        eventManager.fireEvent(Event.Type.SERVER_STOPPING, this)
        Thread {
            embeddedServer!!.stop()
            started = false
            startLatch = CountDownLatch(1)
            eventManager.fireEvent(Event.Type.SERVER_STOPPED, this)
            pathMatcher = PathMatcher()
            exceptionMapper = ExceptionMapper()
            errorMapper = ErrorMapper()
            stopLatch.countDown()
            stopLatch = CountDownLatch(1)
        }.start()
        return this
    }

    fun awaitTermination(): Javalin {
        if (!started) {
            throw IllegalStateException("Server hasn't been stopped. Call stop() before calling this method.")
        }
        try {
            stopLatch.await()
        } catch (e: InterruptedException) {
            log.info("awaitTermination was interrupted")
            Thread.currentThread().interrupt()
        }

        return this
    }

    fun embeddedServer(embeddedServerFactory: EmbeddedServerFactory): Javalin {
        ensureServerHasNotStarted()
        this.embeddedServerFactory = embeddedServerFactory
        return this
    }

    fun embeddedServer(): EmbeddedServer? {
        return embeddedServer
    }

    @Synchronized fun enableStaticFiles(location: String): Javalin {
        ensureServerHasNotStarted()
        Util.notNull("Location cannot be null", location)
        staticFileDirectory = location
        return this
    }

    @Synchronized fun ipAddress(ipAddress: String): Javalin {
        ensureServerHasNotStarted()
        this.ipAddress = ipAddress
        return this
    }

    @Synchronized fun port(port: Int): Javalin {
        ensureServerHasNotStarted()
        this.port = port
        return this
    }

    @Synchronized fun event(eventType: Event.Type, eventListener: EventListener): Javalin {
        ensureServerHasNotStarted()
        eventManager.addEventListener(eventType, eventListener)
        return this
    }

    private fun ensureServerHasNotStarted() {
        if (started) {
            throw IllegalStateException("This must be done before starting the server (adding handlers automatically starts the server)")
        }
    }

    @Synchronized fun port(): Int {
        return if (started) port else -1
    }

    @Synchronized fun <T : Exception> exception(exceptionClass: Class<T>, exceptionHandler: ExceptionHandler<Exception>): Javalin {
        exceptionMapper.put(exceptionClass, exceptionHandler)
        return this
    }

    @Synchronized fun error(statusCode: Int, errorHandler: ErrorHandler): Javalin {
        errorMapper.put(statusCode, errorHandler)
        return this
    }

    fun routes(endpointGroup: ApiBuilder.EndpointGroup): Javalin {
        ApiBuilder.setStaticJavalin(this)
        endpointGroup.addEndpoints()
        ApiBuilder.clearStaticJavalin()
        return this
    }

    fun addHandler(httpMethod: Handler.Type, path: String, handler: Handler): Javalin {
        start()
        pathMatcher.add(httpMethod, path, handler)
        return this
    }

    // HTTP verbs
    operator fun get(path: String, handler: Handler): Javalin {
        return addHandler(Handler.Type.GET, path, handler)
    }

    fun post(path: String, handler: Handler): Javalin {
        return addHandler(Handler.Type.POST, path, handler)
    }

    fun put(path: String, handler: Handler): Javalin {
        return addHandler(Handler.Type.PUT, path, handler)
    }

    fun patch(path: String, handler: Handler): Javalin {
        return addHandler(Handler.Type.PATCH, path, handler)
    }

    fun delete(path: String, handler: Handler): Javalin {
        return addHandler(Handler.Type.DELETE, path, handler)
    }

    fun head(path: String, handler: Handler): Javalin {
        return addHandler(Handler.Type.HEAD, path, handler)
    }

    fun trace(path: String, handler: Handler): Javalin {
        return addHandler(Handler.Type.TRACE, path, handler)
    }

    fun connect(path: String, handler: Handler): Javalin {
        return addHandler(Handler.Type.CONNECT, path, handler)
    }

    fun options(path: String, handler: Handler): Javalin {
        return addHandler(Handler.Type.OPTIONS, path, handler)
    }

    // Secured HTTP verbs
    operator fun get(path: String, handler: Handler, permittedRoles: List<Role>): Javalin {
        return this.get(path, Handler { req, res -> accessManager.manage(handler, req, res, permittedRoles) })
    }

    fun post(path: String, handler: Handler, permittedRoles: List<Role>): Javalin {
        return this.post(path, Handler { req, res -> accessManager.manage(handler, req, res, permittedRoles) })
    }

    fun put(path: String, handler: Handler, permittedRoles: List<Role>): Javalin {
        return this.put(path, Handler { req, res -> accessManager.manage(handler, req, res, permittedRoles) })
    }

    fun patch(path: String, handler: Handler, permittedRoles: List<Role>): Javalin {
        return this.patch(path, Handler { req, res -> accessManager.manage(handler, req, res, permittedRoles) })
    }

    fun delete(path: String, handler: Handler, permittedRoles: List<Role>): Javalin {
        return this.delete(path, Handler { req, res -> accessManager.manage(handler, req, res, permittedRoles) })
    }

    fun head(path: String, handler: Handler, permittedRoles: List<Role>): Javalin {
        return this.head(path, Handler { req, res -> accessManager.manage(handler, req, res, permittedRoles) })
    }

    fun trace(path: String, handler: Handler, permittedRoles: List<Role>): Javalin {
        return this.trace(path, Handler { req, res -> accessManager.manage(handler, req, res, permittedRoles) })
    }

    fun connect(path: String, handler: Handler, permittedRoles: List<Role>): Javalin {
        return this.connect(path, Handler { req, res -> accessManager.manage(handler, req, res, permittedRoles) })
    }

    fun options(path: String, handler: Handler, permittedRoles: List<Role>): Javalin {
        return this.options(path, Handler { req, res -> accessManager.manage(handler, req, res, permittedRoles) })
    }

    // Filters
    fun before(path: String, handler: Handler): Javalin {
        return addHandler(Handler.Type.BEFORE, path, handler)
    }

    fun before(handler: Handler): Javalin {
        return before("/*", handler)
    }

    fun after(path: String, handler: Handler): Javalin {
        return addHandler(Handler.Type.AFTER, path, handler)
    }

    fun after(handler: Handler): Javalin {
        return after("/*", handler)
    }

    // Reverse routing
    fun pathFinder(handler: Handler): String? {
        return pathMatcher.findHandlerPath({ he -> he.handler == handler })
    }

    fun pathFinder(handler: Handler, handlerType: Handler.Type): String? {
        return pathMatcher.findHandlerPath({ he -> he.handler == handler && he.type === handlerType })
    }

}
