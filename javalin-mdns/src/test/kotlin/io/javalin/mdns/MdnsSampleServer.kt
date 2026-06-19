/*
 * Javalin - https://javalin.io
 * Copyright 2024 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.mdns

import io.javalin.Javalin

/**
 * Manual demo - not a test. Run `main` from the IDE (or `mvn exec:java`) and visit the printed URLs.
 */
fun main() {
    val hostname = "javalin-demo"
    val port = System.getenv("PORT")?.toIntOrNull() ?: 80

    val app = Javalin.create { config ->
        config.registerPlugin(MdnsPlugin { it.hostname = hostname })
        config.routes.get("/") { it.result("mDNS demo server is running. Served by $hostname.local") }
    }.start(port)

    val boundPort = app.port()
    val portSuffix = if (boundPort == 80) "" else ":$boundPort"

    println("Try these URLs:")
    println("  http://$hostname.local$portSuffix/")
    println("  http://localhost$portSuffix/")
    println("Note: .local resolution needs a local mDNS resolver (built-in on macOS; Avahi on Linux; Bonjour on Windows).")
}
