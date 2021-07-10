package io.javalin.plugin.graphql

import com.expediagroup.graphql.SchemaGeneratorConfig
import com.expediagroup.graphql.toSchema
import graphql.GraphQL
import io.javalin.http.Context
import io.javalin.plugin.graphql.graphql.GraphQLRun
import io.javalin.websocket.WsMessageContext

class GraphQLHandler(val options: GraphQLOptions) {
    private var graphQL: GraphQL

    init {
        val config = SchemaGeneratorConfig(supportedPackages = options.packages)
        val schema = toSchema(
            config = config,
            queries = options.queries,
            mutations = options.mutations,
            subscriptions = options.subscriptions
        )
        this.graphQL = GraphQL.newGraphQL(schema).build()!!
    }

    fun execute(ctx: Context) {
        val body = ctx.bodyAsClass(Map::class.java)
        this.options.middleHandler(ctx)
        ctx.future(this.genericExecute(body).execute()) { result ->
            if (result != null) {
                ctx.json(result)
            }
        }
    }

    fun execute(ctx: WsMessageContext) {
        val body = ctx.messageAsClass(Map::class.java)
        this.options.wsMiddleHandler(ctx)
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
            .withContext(options.context);
    }


}
