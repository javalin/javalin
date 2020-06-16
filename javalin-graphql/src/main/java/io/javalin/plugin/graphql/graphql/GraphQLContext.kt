package io.javalin.plugin.graphql.graphql

import io.javalin.http.Context
import io.javalin.websocket.WsMessageContext

abstract class GraphQLContext {

    lateinit var context: Context
    lateinit var wscontext: WsMessageContext

    fun setContext(context: Context): GraphQLContext = apply { this.context = context }
    fun setWSContext(wscontext: WsMessageContext): GraphQLContext = apply { this.wscontext = wscontext }
}
