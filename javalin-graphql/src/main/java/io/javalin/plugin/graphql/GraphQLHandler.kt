package io.javalin.plugin.graphql

import com.expediagroup.graphql.SchemaGeneratorConfig
import com.expediagroup.graphql.toSchema
import graphql.GraphQL
import io.javalin.http.Context
import io.javalin.plugin.graphql.graphql.GraphQLRun
import io.javalin.plugin.json.JavalinJson
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
        val result = this.genericExecute(body)
                .execute()
                .thenApplyAsync { JavalinJson.toJson(it) }
        ctx.contentType("application/json").result(result)
    }

    fun execute(ctx: WsMessageContext) {
        val body = ctx.message(Map::class.java)
        this.options.wsMiddleHandler(ctx)
        this.genericExecute(body)
                .subscribe(SubscriberGraphQL(ctx))
    }


    private fun genericExecute(body: Map<*, *>): GraphQLRun {
        val query = body.get("query").toString()
        val variables: Map<String, Any> = if (body["variables"] == null) emptyMap() else body["variables"] as Map<String, Any>

        return GraphQLRun(graphQL)
                .withQuery(query)
                .withVariables(variables)
                .withContext(options.context);
    }


}
