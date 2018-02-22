package io.javalin.base

import io.javalin.Javalin
import io.javalin.core.util.Util

internal abstract class JavalinBase : Javalin {

    protected var started = false

    protected var contextPath = "/"
    protected var port = 7000

    protected fun ensureActionIsPerformedBeforeServerStart(action: String) {
        if (started) {
            throw IllegalStateException(action + " must be done before starting the server")
        }
    }

    override fun contextPath() = contextPath

    override fun contextPath(contextPath: String): Javalin {
        ensureActionIsPerformedBeforeServerStart("Setting the context path")
        this.contextPath = Util.normalizeContextPath(contextPath)
        return this
    }

    override fun port() = port

    override fun port(port: Int): Javalin {
        ensureActionIsPerformedBeforeServerStart("Setting the port")
        this.port = port
        return this
    }
}
