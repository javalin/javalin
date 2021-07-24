package io.javalin.plugin.graphql.server

import com.expediagroup.graphql.server.execution.GraphQLRequestParser
import com.expediagroup.graphql.server.types.GraphQLServerRequest
import io.javalin.http.Context
import java.io.IOException

class JavalinGraphQLRequestParser : GraphQLRequestParser<Context> {

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun parseRequest(context: Context): GraphQLServerRequest = try {
        context.bodyAsClass(GraphQLServerRequest::class.java)
    } catch (e: IOException) {
        throw IOException("Unable to parse GraphQL payload.")
    }
}
