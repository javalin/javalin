package io.javalin.util.mock

import io.javalin.http.Handler
import io.javalin.http.HandlerType

data class Endpoint(
    val method: HandlerType,
    val path: String,
    val handler: Handler
)
