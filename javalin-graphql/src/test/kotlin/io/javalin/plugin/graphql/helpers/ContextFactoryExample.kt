package io.javalin.plugin.graphql.helpers

import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import io.javalin.http.Context

class ContextFactoryExample : GraphQLContextFactory<ContextExample, Context> {
    override suspend fun generateContext(request: Context): ContextExample {
        return ContextExample(request.header("Authorization")?.removePrefix("Beare "))
    }
}
