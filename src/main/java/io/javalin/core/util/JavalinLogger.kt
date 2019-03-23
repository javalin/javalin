/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import org.slf4j.LoggerFactory


object JavalinLogger {
    var enabled = true
    private var log = LoggerFactory.getLogger("io.javalin.Javalin")!!
    // @formatter:off
    @JvmStatic fun info(message: String) = ifEnabled { log.info(message) }
    @JvmStatic fun info(message: String, throwable: Throwable) = ifEnabled { log.info(message, throwable) }
    @JvmStatic fun error(message: String) = ifEnabled { log.error(message) }
    @JvmStatic fun error(message: String, throwable: Throwable) = ifEnabled { log.error(message, throwable) }
    @JvmStatic fun warn(message: String) = ifEnabled { log.warn(message) }
    @JvmStatic fun warn(message: String, throwable: Throwable) = ifEnabled { log.warn(message, throwable) }
    // @formatter:on
    fun ifEnabled(log: () -> Unit) = if(enabled) log.invoke() else Unit
}
