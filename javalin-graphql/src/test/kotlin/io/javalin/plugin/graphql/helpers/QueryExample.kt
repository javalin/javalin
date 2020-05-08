package io.javalin.plugin.graphql.helpers

import com.expediagroup.graphql.annotations.GraphQLContext
import io.javalin.plugin.graphql.graphql.QueryGraphql

class QueryExample(val message: String) : QueryGraphql {
    fun hello(): String = message

    fun context(@GraphQLContext context: ContextExample): ContextExample {
        return context
    }
}
