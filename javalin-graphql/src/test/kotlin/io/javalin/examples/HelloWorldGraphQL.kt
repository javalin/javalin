/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.examples

import com.expediagroup.graphql.annotations.GraphQLDescription
import io.javalin.Javalin
import io.javalin.plugin.graphql.GraphQLOptions
import io.javalin.plugin.graphql.GraphQLPlugin
import io.javalin.plugin.graphql.graphql.QueryGraphql

// More documentation: https://expediagroup.github.io/graphql-kotlin/docs/getting-started
@GraphQLDescription("awesome data")
data class DemoData(
    @GraphQLDescription("key is mandatory")
    val key: String,
    @GraphQLDescription("The widget's value that can be `null`")
    val value: String?
)

@GraphQLDescription("Query Example")
class QueryExample : QueryGraphql {
    fun hello(): String = "Hello world"

    fun demoData(@GraphQLDescription("awesome input") data: DemoData): DemoData = data
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
