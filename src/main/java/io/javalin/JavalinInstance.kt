/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.base.JavalinConfig
import io.javalin.core.JavalinServlet
import io.javalin.core.util.Util
import io.javalin.embeddedserver.EmbeddedServer
import io.javalin.embeddedserver.EmbeddedServerFactory
import io.javalin.embeddedserver.jetty.EmbeddedJettyFactory
import io.javalin.event.EventListener
import io.javalin.event.EventManager
import io.javalin.event.EventType
import org.slf4j.LoggerFactory

internal class JavalinInstance : JavalinConfig() {

    companion object {
        private val log = LoggerFactory.getLogger(Javalin::class.java)
    }

    private lateinit var embeddedServer: EmbeddedServer
    private var embeddedServerFactory: EmbeddedServerFactory = EmbeddedJettyFactory()


    private val eventManager = EventManager()

    override fun event(type: EventType, listener: EventListener): Javalin {
        ensureActionIsPerformedBeforeServerStart("Event-mapping")
        eventManager.listenerMap[type]?.add(listener)
        return this
    }

    override fun embeddedServer(embeddedServerFactory: EmbeddedServerFactory): Javalin {
        ensureActionIsPerformedBeforeServerStart("Setting a custom server")
        this.embeddedServerFactory = embeddedServerFactory
        return this
    }

    override fun start(): Javalin {
        if (!started) {
            log.info(Util.javalinBanner())
            Util.printHelpfulMessageIfLoggerIsMissing()
            Util.noServerHasBeenStarted = false
            eventManager.fireEvent(EventType.SERVER_STARTING, this)
            try {
                embeddedServer = embeddedServerFactory.create(JavalinServlet(
                    contextPath,
                    pathMatcher,
                    exceptionMapper,
                    errorMapper,
                    pathWsHandlers,
                    logLevel,
                    dynamicGzipEnabled,
                    defaultContentType,
                    defaultCharacterEncoding,
                    maxRequestCacheBodySize
                ), staticFileConfigs)
                log.info("Starting JavalinInstance ...")
                port = embeddedServer.start(port)
                log.info("JavalinInstance has started \\o/")
                started = true
                eventManager.fireEvent(EventType.SERVER_STARTED, this)
            } catch (e: Exception) {
                log.error("Failed to start JavalinInstance", e)
                eventManager.fireEvent(EventType.SERVER_START_FAILED, this)
            }

        }
        return this
    }

    override fun stop(): Javalin {
        eventManager.fireEvent(EventType.SERVER_STOPPING, this)
        log.info("Stopping JavalinInstance ...")
        try {
            embeddedServer.stop()
        } catch (e: Exception) {
            log.error("JavalinInstance failed to stop gracefully", e)
        }

        log.info("JavalinInstance has stopped")
        eventManager.fireEvent(EventType.SERVER_STOPPED, this)
        return this
    }

    // package private method used for testing
    public fun embeddedServer(): EmbeddedServer? {
        return embeddedServer
    }

    // package private method used for testing
    public fun clearMatcherAndMappers() {
        pathMatcher.handlerEntries.clear()
        errorMapper.errorHandlerMap.clear()
        exceptionMapper.exceptionMap.clear()
    }
}
