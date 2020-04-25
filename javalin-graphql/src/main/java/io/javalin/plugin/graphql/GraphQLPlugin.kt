package io.javalin.plugin.graphql

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginLifecycleInit

class GraphQLPlugin(private val options: GraphQLOptions) : Plugin, PluginLifecycleInit {

    lateinit var graphQLHandler: GraphQLHandler

    override fun init(app: Javalin) {
        graphQLHandler = GraphQLHandler(options)
    }

    override fun apply(app: Javalin) {
        app.get(options.path) {
            it.contentType("text/html; charset=UTF-8")
                    .result(
                            GraphQLPlugin::class.java.getResourceAsStream("graphqli/index.html")
                    )
        }
        app.post(options.path) { ctx ->
            graphQLHandler.execute(ctx)
        }
        app.ws(options.path) { ws ->
            ws.onMessage { ctx -> graphQLHandler.execute(ctx) }
        }
    }
}
