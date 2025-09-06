/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.json.JsonMapper
import io.javalin.security.RouteRole
import io.javalin.validation.Validation

/**
 * Data class that stores essential information extracted during WebSocket upgrade.
 * This replaces the need to carry over the full servlet context for Jetty 12 migration.
 */
data class WsUpgradeData(
    val matchedPath: String,
    val pathParamMap: Map<String, String>,
    val queryString: String?,
    val queryParamMap: Map<String, List<String>>,
    val headerMap: Map<String, String>,
    val cookieMap: Map<String, String>,
    val attributeMap: Map<String, Any?>,
    val sessionAttributeMap: Map<String, Any?>,
    val routeRoles: Set<RouteRole>,
    val jsonMapper: JsonMapper,
    val validation: Validation
)