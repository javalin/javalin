package io.javalin.plugin.graphql.server

import com.expediagroup.graphql.server.execution.*
import io.javalin.http.Context
import io.javalin.plugin.graphql.GraphQLPluginBuilder

class JavalinGraphQLServer(contextFactory: GraphQLContextFactory<*, Context>, requestHandler: GraphQLRequestHandler) : GraphQLServer<Context>(
    JavalinGraphQLRequestParser(), contextFactory, requestHandler) {
    companion object {
        fun create(builder: GraphQLPluginBuilder<*>): JavalinGraphQLServer {
            val dataLoaderRegistryFactory = builder.toJavalinDataLoaderRegistryFactory()
            val requestHandler = GraphQLRequestHandler(builder.getSchema(), dataLoaderRegistryFactory)

            return JavalinGraphQLServer(builder.contextFactory, requestHandler)
        }
    }
}

