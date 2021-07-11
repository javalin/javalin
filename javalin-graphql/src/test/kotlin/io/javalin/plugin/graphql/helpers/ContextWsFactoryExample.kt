package io.javalin.plugin.graphql.helpers

import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import io.javalin.http.Context
import io.javalin.websocket.WsMessageContext

class ContextWsFactoryExample : GraphQLContextFactory<ContextExample, WsMessageContext> {
    override suspend fun generateContext(request: WsMessageContext): ContextExample {
        return ContextExample(request.header("Authorization")?.removePrefix("Beare "))
    }
}
