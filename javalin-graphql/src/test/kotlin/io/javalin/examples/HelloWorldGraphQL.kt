/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.examples

import io.javalin.Javalin
import io.javalin.plugin.graphql.GraphQLOptions
import io.javalin.plugin.graphql.GraphQLPlugin
import io.javalin.plugin.graphql.graphql.QueryGraphql

class QueryExample : QueryGraphql {
    fun hello(): String = "Hello world"
}

fun main() {
    val app = Javalin.create {
        val graphQLOption = GraphQLOptions("/graphql")
                .addPackage("io.javalin.examples")
                .register(QueryExample())
        it.registerPlugin(GraphQLPlugin(graphQLOption))
    }

    app.start()
}
