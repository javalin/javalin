package io.javalin.router

import io.javalin.http.ExceptionHandler
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.security.RouteRole
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsExceptionHandler
import java.util.function.Consumer

interface Router<ROUTER : Router<ROUTER, SETUP>, SETUP> {

    fun <E : Exception> exception(exceptionClass: Class<E>, exceptionHandler: ExceptionHandler<in E>): ROUTER

    fun error(status: Int, contentType: String, handler: Handler): ROUTER

    fun addHandler(handlerType: HandlerType, path: String, handler: Handler, vararg roles: RouteRole): ROUTER

    fun <E : Exception> wsException(exceptionClass: Class<E>, exceptionHandler: WsExceptionHandler<in E>): ROUTER

    fun ws(path: String, ws: Consumer<WsConfig>, vararg roles: RouteRole): ROUTER

    fun wsAfter(path: String, wsConfig: Consumer<WsConfig>): ROUTER

}

interface RouterFactory<ROUTER : Router<ROUTER, SETUP>, SETUP > {
    fun create(internalRouter: InternalRouter, setup: Consumer<SETUP>): ROUTER
}
