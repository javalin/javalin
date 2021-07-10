package io.javalin.plugin.graphql

import com.expediagroup.graphql.server.execution.*
import io.javalin.http.Context

class JavalinGraphQLServer(contextFactory: GraphQLContextFactory<*, Context>, requestHandler: GraphQLRequestHandler) : GraphQLServer<Context>(JavalinGraphQLRequestParser(), contextFactory, requestHandler) {
    companion object {
        fun create(builder: GraphQLPluginBuilder<*>): JavalinGraphQLServer {
            val dataLoaderRegistryFactory = builder.toJavalinDataLoaderRegistryFactory()
            val requestHandler = GraphQLRequestHandler(builder.createSchema(), dataLoaderRegistryFactory)

            return JavalinGraphQLServer(builder.contextFactory, requestHandler)
        }
    }
}

