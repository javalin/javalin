package io.javalin.core.util

import io.javalin.Javalin
import org.slf4j.LoggerFactory
import java.util.*

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

    // we want to log some stuff from JettyResourceHandler after server starts,
    // but we don't really want those classes to be aware of each other ...
    // someone should call christina aguilera, because this is dirrty.
    private val delayed = ArrayDeque<(Unit) -> Unit>()
    internal fun addDelayed(action: (Unit) -> Unit) = delayed.add(action)
    internal fun logAllDelayed() { while (delayed.size > 0) delayed.poll().invoke(Unit) }
}
// @formatter:on
