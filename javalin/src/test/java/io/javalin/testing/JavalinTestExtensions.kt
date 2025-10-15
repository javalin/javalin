@file:Suppress("unused")

package io.javalin.testing

import io.javalin.Javalin
import io.javalin.http.*
import io.javalin.router.Endpoint
import io.javalin.security.RouteRole
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsExceptionHandler
import io.javalin.websocket.WsHandlerType
import java.util.function.Consumer

/**
 * Test extension functions that provide the old app.verb() syntax for testing
 * while using the new config.routes API under the hood.
 *
 * These extensions allow tests to maintain their ergonomic syntax:
 * app.get("/path") { ctx -> ctx.result("Hello") }
 *
 * Instead of requiring the more verbose:
 * app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(...)
 */

// Helper functions to reduce repetition
private fun Javalin.addHttpEndpoint(handlerType: HandlerType, path: String, handler: Handler): Javalin = apply {
    unsafeConfig().pvt.internalRouter.addHttpEndpoint(
        Endpoint(handlerType, path, emptySet(), handler)
    )
}

private fun Javalin.addHttpEndpointWithRoles(handlerType: HandlerType, path: String, handler: Handler, vararg roles: RouteRole): Javalin = apply {
    unsafeConfig().pvt.internalRouter.addHttpEndpoint(
        Endpoint.create(handlerType, path)
            .addMetadata(io.javalin.security.Roles(roles.toSet()))
            .handler(handler)
    )
}

private fun Javalin.addWsHandler(wsHandlerType: WsHandlerType, path: String, wsConfig: Consumer<WsConfig>): Javalin = apply {
    unsafeConfig().pvt.internalRouter.addWsHandler(wsHandlerType, path, wsConfig)
}

private fun Javalin.addWsHandlerWithRoles(wsHandlerType: WsHandlerType, path: String, wsConfig: Consumer<WsConfig>, vararg roles: RouteRole): Javalin = apply {
    unsafeConfig().pvt.internalRouter.addWsHandler(wsHandlerType, path, wsConfig, *roles)
}

// HTTP Methods - dramatically simplified!
fun Javalin.get(path: String, handler: Handler): Javalin = addHttpEndpoint(HandlerType.GET, path, handler)
fun Javalin.post(path: String, handler: Handler): Javalin = addHttpEndpoint(HandlerType.POST, path, handler)
fun Javalin.put(path: String, handler: Handler): Javalin = addHttpEndpoint(HandlerType.PUT, path, handler)
fun Javalin.patch(path: String, handler: Handler): Javalin = addHttpEndpoint(HandlerType.PATCH, path, handler)
fun Javalin.delete(path: String, handler: Handler): Javalin = addHttpEndpoint(HandlerType.DELETE, path, handler)
fun Javalin.head(path: String, handler: Handler): Javalin = addHttpEndpoint(HandlerType.HEAD, path, handler)
fun Javalin.options(path: String, handler: Handler): Javalin = addHttpEndpoint(HandlerType.OPTIONS, path, handler)

// HTTP Methods with roles - dramatically simplified!
fun Javalin.get(path: String, handler: Handler, vararg roles: RouteRole): Javalin = addHttpEndpointWithRoles(HandlerType.GET, path, handler, *roles)
fun Javalin.post(path: String, handler: Handler, vararg roles: RouteRole): Javalin = addHttpEndpointWithRoles(HandlerType.POST, path, handler, *roles)
fun Javalin.put(path: String, handler: Handler, vararg roles: RouteRole): Javalin = addHttpEndpointWithRoles(HandlerType.PUT, path, handler, *roles)
fun Javalin.patch(path: String, handler: Handler, vararg roles: RouteRole): Javalin = addHttpEndpointWithRoles(HandlerType.PATCH, path, handler, *roles)
fun Javalin.delete(path: String, handler: Handler, vararg roles: RouteRole): Javalin = addHttpEndpointWithRoles(HandlerType.DELETE, path, handler, *roles)
fun Javalin.head(path: String, handler: Handler, vararg roles: RouteRole): Javalin = addHttpEndpointWithRoles(HandlerType.HEAD, path, handler, *roles)
fun Javalin.options(path: String, handler: Handler, vararg roles: RouteRole): Javalin = addHttpEndpointWithRoles(HandlerType.OPTIONS, path, handler, *roles)

// Before/After handlers - simplified!
fun Javalin.before(handler: Handler): Javalin = before("*", handler)
fun Javalin.before(path: String, handler: Handler): Javalin = addHttpEndpoint(HandlerType.BEFORE, path, handler)
fun Javalin.beforeMatched(handler: Handler): Javalin = beforeMatched("*", handler)
fun Javalin.beforeMatched(path: String, handler: Handler): Javalin = addHttpEndpoint(HandlerType.BEFORE_MATCHED, path, handler)
fun Javalin.after(handler: Handler): Javalin = after("*", handler)
fun Javalin.after(path: String, handler: Handler): Javalin = addHttpEndpoint(HandlerType.AFTER, path, handler)
fun Javalin.afterMatched(handler: Handler): Javalin = afterMatched("*", handler)
fun Javalin.afterMatched(path: String, handler: Handler): Javalin = addHttpEndpoint(HandlerType.AFTER_MATCHED, path, handler)

// Exception and error handlers - simplified!
fun <E : Exception> Javalin.exception(exceptionClass: Class<E>, exceptionHandler: ExceptionHandler<in E>): Javalin = apply {
    unsafeConfig().pvt.internalRouter.addHttpExceptionHandler(exceptionClass, exceptionHandler)
}

fun Javalin.error(status: Int, handler: Handler): Javalin = apply {
    unsafeConfig().pvt.internalRouter.addHttpErrorHandler(status, "*", handler)
}

fun Javalin.error(status: HttpStatus, handler: Handler): Javalin = error(status.code, handler)

fun Javalin.error(status: Int, contentType: String, handler: Handler): Javalin = apply {
    unsafeConfig().pvt.internalRouter.addHttpErrorHandler(status, contentType, handler)
}

// Server-Sent Events - simplified!
fun Javalin.sse(path: String, client: Consumer<io.javalin.http.sse.SseClient>): Javalin = apply {
    unsafeConfig().pvt.internalRouter.addHttpEndpoint(
        Endpoint(HandlerType.GET, path, emptySet(), io.javalin.http.sse.SseHandler(clientConsumer = client))
    )
}

// WebSocket handlers - dramatically simplified!
fun Javalin.ws(path: String, wsConfig: Consumer<WsConfig>): Javalin = addWsHandler(WsHandlerType.WEBSOCKET, path, wsConfig)
fun Javalin.ws(path: String, wsConfig: Consumer<WsConfig>, vararg roles: RouteRole): Javalin = addWsHandlerWithRoles(WsHandlerType.WEBSOCKET, path, wsConfig, *roles)
fun Javalin.wsBefore(handler: Consumer<WsConfig>): Javalin = wsBefore("*", handler)
fun Javalin.wsBefore(path: String, wsConfig: Consumer<WsConfig>): Javalin = addWsHandler(WsHandlerType.WEBSOCKET_BEFORE, path, wsConfig)
fun Javalin.wsAfter(handler: Consumer<WsConfig>): Javalin = wsAfter("*", handler)
fun Javalin.wsAfter(path: String, wsConfig: Consumer<WsConfig>): Javalin = addWsHandler(WsHandlerType.WEBSOCKET_AFTER, path, wsConfig)
fun Javalin.wsBeforeUpgrade(handler: Handler): Javalin = wsBeforeUpgrade("*", handler)
fun Javalin.wsBeforeUpgrade(path: String, handler: Handler): Javalin = addHttpEndpoint(HandlerType.WEBSOCKET_BEFORE_UPGRADE, path, handler)
fun Javalin.wsAfterUpgrade(handler: Handler): Javalin = wsAfterUpgrade("*", handler)
fun Javalin.wsAfterUpgrade(path: String, handler: Handler): Javalin = addHttpEndpoint(HandlerType.WEBSOCKET_AFTER_UPGRADE, path, handler)
fun <E : Exception> Javalin.wsException(exceptionClass: Class<E>, exceptionHandler: WsExceptionHandler<in E>): Javalin = apply {
    unsafeConfig().pvt.internalRouter.addWsExceptionHandler(exceptionClass, exceptionHandler)
}

// Custom HTTP methods - simplified!
fun Javalin.addHttpHandler(handlerType: HandlerType, path: String, handler: Handler): Javalin = addHttpEndpoint(handlerType, path, handler)
fun Javalin.addHttpHandler(handlerType: HandlerType, path: String, handler: Handler, vararg roles: RouteRole): Javalin = addHttpEndpointWithRoles(handlerType, path, handler, *roles)
