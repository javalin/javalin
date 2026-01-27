/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.jetty

import org.slf4j.LoggerFactory
import org.slf4j.event.Level

internal object JettyLoggingUtil {

    private const val JETTY_LOGGER = "org.eclipse.jetty"

    // slf4j-simple log levels: TRACE=0, DEBUG=10, INFO=20, WARN=30, ERROR=40
    private val simpleLoggerLevels = mapOf("TRACE" to 0, "DEBUG" to 10, "INFO" to 20, "WARN" to 30, "ERROR" to 40)
    private val simpleLoggerLevelNames = simpleLoggerLevels.entries.associate { (k, v) -> v to k }

    private val jettyLoggerNames = listOf(
        JETTY_LOGGER,
        "org.eclipse.jetty.server.Server",
        "org.eclipse.jetty.server.handler.ContextHandler",
        "org.eclipse.jetty.server.AbstractConnector",
        "org.eclipse.jetty.session.DefaultSessionIdManager",
        "org.eclipse.jetty.ee10.servlet.ServletContextHandler"
    )

    /**
     * Executes the given block with Jetty's log level temporarily changed if level is non-null.
     * If level is null, just executes the block without changing log level.
     * Saves and restores the original log level to avoid overriding user's logging configuration.
     * Works with Logback, Log4j2, and slf4j-simple via reflection. Best-effort - silently runs block if unsupported.
     */
    inline fun <T> withJettyLogLevel(level: Level?, block: () -> T): T {
        if (level == null) return block()
        val originalLevel = getJettyLogLevel()
        setJettyLogLevel(level.name)
        try {
            return block()
        } finally {
            originalLevel?.let { setJettyLogLevel(it) }
        }
    }

    private fun getJettyLogLevel(): String? =
        runCatching { tryLogbackGetLevel() }.getOrNull()
            ?: runCatching { tryLog4j2GetLevel() }.getOrNull()
            ?: runCatching { trySlf4jSimpleGetLevel() }.getOrNull()

    private fun setJettyLogLevel(levelName: String) {
        runCatching { tryLogbackSetLevel(levelName) }.getOrElse {
            runCatching { tryLog4j2SetLevel(levelName) }.getOrElse {
                runCatching { trySlf4jSimpleSetLevel(levelName) }
            }
        }
    }

    // Logback
    private fun tryLogbackGetLevel(): String? {
        val loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext")
        val factory = LoggerFactory.getILoggerFactory()
        if (!loggerContextClass.isInstance(factory)) return null
        val logger = factory.javaClass.getMethod("getLogger", String::class.java).invoke(factory, JETTY_LOGGER)
        return logger.javaClass.getMethod("getLevel").invoke(logger)?.toString()
    }

    private fun tryLogbackSetLevel(levelName: String) {
        val loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext")
        val factory = LoggerFactory.getILoggerFactory()
        if (!loggerContextClass.isInstance(factory)) error("Not Logback")
        val logger = factory.javaClass.getMethod("getLogger", String::class.java).invoke(factory, JETTY_LOGGER)
        val levelClass = Class.forName("ch.qos.logback.classic.Level")
        val level = levelClass.getField(levelName).get(null)
        logger.javaClass.getMethod("setLevel", levelClass).invoke(logger, level)
    }

    // Log4j2
    private fun tryLog4j2GetLevel(): String? {
        val logManager = Class.forName("org.apache.logging.log4j.LogManager")
        val logger = logManager.getMethod("getLogger", String::class.java).invoke(null, JETTY_LOGGER)
        return logger.javaClass.getMethod("getLevel").invoke(logger)?.toString()
    }

    private fun tryLog4j2SetLevel(levelName: String) {
        val configurator = Class.forName("org.apache.logging.log4j.core.config.Configurator")
        val levelClass = Class.forName("org.apache.logging.log4j.Level")
        val level = levelClass.getField(levelName).get(null)
        configurator.getMethod("setLevel", String::class.java, levelClass).invoke(null, JETTY_LOGGER, level)
    }

    // slf4j-simple
    private fun trySlf4jSimpleGetLevel(): String? {
        val simpleLoggerClass = Class.forName("org.slf4j.simple.SimpleLogger")
        val logger = LoggerFactory.getLogger(JETTY_LOGGER)
        if (!simpleLoggerClass.isInstance(logger)) return null
        val field = simpleLoggerClass.getDeclaredField("currentLogLevel").apply { isAccessible = true }
        return simpleLoggerLevelNames[field.getInt(logger)]
    }

    private fun trySlf4jSimpleSetLevel(levelName: String) {
        val simpleLoggerClass = Class.forName("org.slf4j.simple.SimpleLogger")
        val levelInt = simpleLoggerLevels[levelName] ?: error("Unknown level: $levelName")
        val field = simpleLoggerClass.getDeclaredField("currentLogLevel").apply { isAccessible = true }
        jettyLoggerNames
            .map { LoggerFactory.getLogger(it) }
            .filter { simpleLoggerClass.isInstance(it) }
            .forEach { field.setInt(it, levelInt) }
    }

}

