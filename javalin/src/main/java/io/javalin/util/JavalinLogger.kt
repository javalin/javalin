package io.javalin.util

import io.javalin.Javalin
import org.slf4j.LoggerFactory

// @formatter:off
object JavalinLogger {

    private val log = LoggerFactory.getLogger(Javalin::class.java)!!

    @JvmField var enabled = true
    @JvmField var startupInfo = true

    @JvmStatic fun startup(message: String) {
        if (!enabled) return
        if (startupInfo) log.info(message)
    }

    @JvmOverloads @JvmStatic fun info(message: String, throwable: Throwable? = null) {
        if (!enabled) return
        if (throwable != null) log.info(message, throwable) else log.info(message)
    }

    @JvmOverloads @JvmStatic fun warn(message: String, throwable: Throwable? = null) {
        if (!enabled) return
        if (throwable != null) log.warn(message, throwable) else log.warn(message)
    }

    @JvmOverloads @JvmStatic fun error(message: String, throwable: Throwable? = null) {
        if (!enabled) return
        if (throwable != null) log.error(message, throwable) else log.error(message)
    }

    @JvmOverloads @JvmStatic fun debug(message: String, throwable: Throwable? = null) {
        if (!enabled) return
        if (throwable != null) log.debug(message, throwable) else log.debug(message)
    }

}
// @formatter:on
