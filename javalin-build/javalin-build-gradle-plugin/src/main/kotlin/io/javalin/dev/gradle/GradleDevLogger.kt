package io.javalin.dev.gradle

import io.javalin.dev.log.JavalinDevLogger
import org.gradle.api.logging.Logger

class GradleDevLogger(private val logger: Logger) : JavalinDevLogger {
    override fun info(message: String) = logger.lifecycle(message)
    override fun warn(message: String) = logger.warn(message)
    override fun error(message: String) = logger.error(message)
    override fun debug(message: String) = logger.debug(message)
}
