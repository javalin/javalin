package io.javalin.plugin.graphql.helpers

import io.javalin.plugin.graphql.graphql.GraphQLContext


class ContextExample: GraphQLContext() {
    val hi = "Hi"
    val hello = "Hello World"
}
