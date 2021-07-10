package io.javalin.plugin.graphql

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginLifecycleInit
import io.javalin.http.BadRequestResponse
import io.javalin.plugin.json.jsonMapper
import kotlinx.coroutines.future.future
import kotlinx.coroutines.runBlocking
import org.eclipse.jetty.http.HttpStatus
import reactor.core.publisher.toMono

class GraphQLPlugin(private val builder: GraphQLPluginBuilder<*>) : Plugin, PluginLifecycleInit {

    constructor(options: GraphQLOptions) : this(GraphQLPluginBuilder.create(options)) {
        GraphQLPluginBuilder
    }

    private val graphQLHandler: GraphQLHandler = GraphQLHandler(builder.createSchema())


    override fun apply(app: Javalin) {
        val server = JavalinGraphQLServer.create(builder)
        app.get(builder.path) {
            it.contentType("text/html; charset=UTF-8")
                .result(
                    GraphQLPlugin::class.java.getResourceAsStream("graphqli/index.html")
                )
        }
        app.post(builder.path) { ctx ->
            val response = runBlocking { server.execute(ctx) }
            if (response != null) {
                ctx.json(response)
            } else {
                ctx.status(HttpStatus.BAD_REQUEST_400).json(mapOf("error" to "Invalid request"))
            }
        }
        app.ws(builder.path) { ws ->
            ws.onMessage { ctx -> graphQLHandler.execute(ctx) }
        }
    }

    override fun init(app: Javalin) {

    }

}
