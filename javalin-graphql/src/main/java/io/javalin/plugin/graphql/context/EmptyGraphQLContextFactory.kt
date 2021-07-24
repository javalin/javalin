package io.javalin.plugin.graphql.context

import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import io.javalin.http.Context

class EmptyGraphQLContextFactory : GraphQLContextFactory<EmptyGraphQLContext, Context> {
    override suspend fun generateContext(request: Context): EmptyGraphQLContext {
        return EmptyGraphQLContext()
    }
}
