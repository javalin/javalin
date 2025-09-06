package io.javalin.http.servlet

import io.javalin.security.RouteRole
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class JavalinWsServletContext(
    cfg: JavalinServletContextConfig,
    req: HttpServletRequest,
    res: HttpServletResponse,
    routeRoles: Set<RouteRole>,
    matchedPath: String,
    pathParamMap: Map<String, String>,
) : JavalinServletContext(
    cfg = cfg,
    req = req,
    res = res,
    routeRoles = routeRoles,
    matchedPath = matchedPath,
    pathParamMap = pathParamMap,
) {
    val extractedData = UpgradeRequestData(this)
}

data class UpgradeRequestData(val context: JavalinWsServletContext) {
    val requestUri = context.req().requestURI.removePrefix(context.req().contextPath)
    val host = context.host()
    val queryParamMap = context.queryParamMap()
    val headerMap = context.headerMap()
    val cookieMap = context.cookieMap()
    val attributeMap = context.attributeMap().toMutableMap()
    val sessionAttributeMap = context.sessionAttributeMap()
}
