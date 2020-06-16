package io.javalin.plugin.graphql

import com.expediagroup.graphql.TopLevelObject
import io.javalin.plugin.graphql.graphql.GraphQLContext
import io.javalin.plugin.graphql.graphql.MutationGraphql
import io.javalin.plugin.graphql.graphql.QueryGraphql
import io.javalin.plugin.graphql.graphql.SubscriptionGraphql

class GraphQLOptions(val path: String, val context: GraphQLContext? = null) {
    var queries: MutableList<TopLevelObject> = mutableListOf()
    var mutations: MutableList<TopLevelObject> = mutableListOf()
    var subscriptions: MutableList<TopLevelObject> = mutableListOf()
    var packages: MutableList<String> = mutableListOf()

    fun register(vararg queries: QueryGraphql) = apply {
        this.queries.addAll(queries.map { TopLevelObject(it) })
    }

    fun register(vararg mutations: MutationGraphql) = apply {
        this.mutations.addAll(mutations.map { TopLevelObject(it) })
    }

    fun register(vararg subscriptions: SubscriptionGraphql) = apply {
        this.subscriptions.addAll(subscriptions.map { TopLevelObject(it) })
    }

    fun addPackage(`package`: String) = apply {
        this.packages.add(`package`)
    }
}
