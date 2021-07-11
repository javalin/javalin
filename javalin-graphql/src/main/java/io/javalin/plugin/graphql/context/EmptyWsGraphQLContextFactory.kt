package io.javalin.plugin.graphql.context

import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import io.javalin.websocket.WsMessageContext

class EmptyWsGraphQLContextFactory : GraphQLContextFactory<EmptyGraphQLContext, WsMessageContext> {
    override suspend fun generateContext(request: WsMessageContext): EmptyGraphQLContext {
        return EmptyGraphQLContext()
    }
}
