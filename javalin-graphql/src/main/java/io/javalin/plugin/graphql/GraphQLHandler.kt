package io.javalin.plugin.graphql

import graphql.GraphQL
import io.javalin.plugin.graphql.graphql.GraphQLRun
import io.javalin.websocket.WsMessageContext

class GraphQLHandler(private val graphQL: GraphQL) {


    fun execute(ctx: WsMessageContext) {
        val body = ctx.messageAsClass(Map::class.java)
        this.genericExecute(body)
            .subscribe(SubscriberGraphQL(ctx))
    }


    private fun genericExecute(body: Map<*, *>): GraphQLRun {
        val query = body.get("query").toString()
        val variables: Map<String, Any> = if (body["variables"] == null) emptyMap() else body["variables"] as Map<String, Any>
        val operationName = body.get("operationName")?.toString()

        return GraphQLRun(graphQL)
            .withQuery(query)
            .withVariables(variables)
            .withOperationName(operationName)
    }


}
