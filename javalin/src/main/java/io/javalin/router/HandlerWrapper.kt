package io.javalin.router

import io.javalin.http.Handler

fun interface HandlerWrapper {
    fun wrap(endpoint: Endpoint): Handler
}

