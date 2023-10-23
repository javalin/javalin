package io.javalin.router

import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.security.RouteRole

class Endpoint @JvmOverloads constructor(
    val method: HandlerType,
    val path: String,
    val roles: Set<RouteRole> = emptySet(),
    val handler: Handler
)
