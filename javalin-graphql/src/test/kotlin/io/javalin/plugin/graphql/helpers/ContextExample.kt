package io.javalin.plugin.graphql.helpers

import com.expediagroup.graphql.generator.execution.GraphQLContext

data class ContextExample(val authorization: String? = null) : GraphQLContext {
    val isValid = authorization != null
}
