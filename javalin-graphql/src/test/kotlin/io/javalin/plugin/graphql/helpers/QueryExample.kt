package io.javalin.plugin.graphql.helpers

import io.javalin.plugin.graphql.graphql.QueryGraphql

class QueryExample(val message: String) : QueryGraphql {
    fun hello(): String = message

    fun echo(message: String): String = message

    fun isAuthorized(context: ContextExample?): Boolean {
        return context != null && context.isValid
    }
}
