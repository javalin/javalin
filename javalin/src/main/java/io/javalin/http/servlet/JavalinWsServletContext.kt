package io.javalin.http.servlet

import io.javalin.security.RouteRole
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.websocket.api.Session
import java.util.*

class JavalinWsServletContext(
    cfg: JavalinServletContextConfig,
    req: HttpServletRequest,
    res: HttpServletResponse,
    routeRoles: Set<RouteRole> = emptySet(),
    matchedPath: String = "",
    pathParamMap: Map<String, String> = emptyMap(),
) : JavalinServletContext(
    cfg = cfg,
    req = req,
    res = res,
    routeRoles = routeRoles,
    matchedPath = matchedPath,
    pathParamMap = pathParamMap,
) {
    val extractedData = UpgradeRequestData(this)
    fun attach(session: Session) = apply { this.extractedData.session = session }
}

/*
 * Jetty will recycle the HttpServletRequest/Response objects, so we need to extract all the
 * data we need before we lose access to it.
 */
data class UpgradeRequestData(val context: JavalinWsServletContext) {
    lateinit var session: Session
    val sessionId: String = UUID.randomUUID().toString()
    val requestUri = context.req().requestURI.removePrefix(context.req().contextPath)
    val host = context.host()
    val queryParamMap = context.queryParamMap()
    val headerMap = context.headerMap()
    val cookieMap = context.cookieMap()
    val attributeMap = context.attributeMap().toMutableMap()
    val sessionAttributeMap = context.sessionAttributeMap()
}
