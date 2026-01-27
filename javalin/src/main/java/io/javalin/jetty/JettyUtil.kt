package io.javalin.jetty

import io.javalin.config.JavalinState
import io.javalin.http.servlet.ServletEntry
import io.javalin.util.Util
import org.eclipse.jetty.ee10.websocket.server.config.JettyWebSocketServletContainerInitializer
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

internal object JettyUtil {

    fun createJettyServletWithWebsocketsIfAvailable(cfg: JavalinState): ServletEntry? =
        when {
            Util.classExists("org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServlet") ->
                ServletEntry(JettyWebSocketServletContainerInitializer(null), JavalinJettyServlet(cfg))
            else ->
                null
        }

    private const val JETTY_LOGGER = "org.eclipse.jetty"

    /**
     * Executes the given block with Jetty's log level temporarily changed if level is non-null.
     * If level is null, just executes the block without changing log level.
     * Works with Logback, Log4j2, and slf4j-simple via reflection. Best-effort - silently runs block if unsupported.
     */
    inline fun <T> withJettyLogLevel(level: Level?, block: () -> T): T {
        if (level == null) return block()
        setJettyLogLevel(level.name)
        try {
            return block()
        } finally {
            setJettyLogLevel(Level.INFO.name)
        }
    }

    fun setJettyLogLevel(levelName: String) {
        try {
            if (tryLogback(levelName)) return
            if (tryLog4j2(levelName)) return
            if (trySlf4jSimple(levelName)) return
        } catch (_: Exception) {
            // Best-effort - silently ignore if we can't configure logging
        }
    }

    private fun tryLogback(levelName: String): Boolean = try {
        val loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext")
        val factory = LoggerFactory.getILoggerFactory()
        if (!loggerContextClass.isInstance(factory)) false
        else {
            val logger = factory.javaClass.getMethod("getLogger", String::class.java)
                .invoke(factory, JETTY_LOGGER)
            val levelClass = Class.forName("ch.qos.logback.classic.Level")
            val level = levelClass.getField(levelName).get(null)
            logger.javaClass.getMethod("setLevel", levelClass).invoke(logger, level)
            true
        }
    } catch (_: Exception) { false }

    private fun tryLog4j2(levelName: String): Boolean = try {
        val configurator = Class.forName("org.apache.logging.log4j.core.config.Configurator")
        val levelClass = Class.forName("org.apache.logging.log4j.Level")
        val level = levelClass.getField(levelName).get(null)
        configurator.getMethod("setLevel", String::class.java, levelClass)
            .invoke(null, JETTY_LOGGER, level)
        true
    } catch (_: Exception) { false }

    // slf4j-simple log levels: TRACE=0, DEBUG=10, INFO=20, WARN=30, ERROR=40
    private val simpleLoggerLevels = mapOf("TRACE" to 0, "DEBUG" to 10, "INFO" to 20, "WARN" to 30, "ERROR" to 40)

    private fun trySlf4jSimple(levelName: String): Boolean = try {
        val simpleLoggerClass = Class.forName("org.slf4j.simple.SimpleLogger")
        val factory = LoggerFactory.getILoggerFactory()
        // Get all Jetty loggers and set their level via reflection on currentLogLevel field
        val levelInt = simpleLoggerLevels[levelName] ?: return false
        listOf(
            "org.eclipse.jetty",
            "org.eclipse.jetty.server.Server",
            "org.eclipse.jetty.server.handler.ContextHandler",
            "org.eclipse.jetty.server.AbstractConnector",
            "org.eclipse.jetty.session.DefaultSessionIdManager",
            "org.eclipse.jetty.ee10.servlet.ServletContextHandler"
        ).forEach { loggerName ->
            val logger = LoggerFactory.getLogger(loggerName)
            if (simpleLoggerClass.isInstance(logger)) {
                val field = simpleLoggerClass.getDeclaredField("currentLogLevel")
                field.isAccessible = true
                field.setInt(logger, levelInt)
            }
        }
        true
    } catch (_: Exception) { false }

}
