package io.javalin.plugin.graphql

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.execution.GraphQLContext
import com.expediagroup.graphql.generator.toSchema
import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import com.expediagroup.graphql.server.execution.KotlinDataLoader
import graphql.GraphQL
import io.javalin.http.Context
import io.javalin.plugin.graphql.context.EmptyGraphQLContextFactory
import io.javalin.plugin.graphql.graphql.MutationGraphql
import io.javalin.plugin.graphql.graphql.QueryGraphql
import io.javalin.plugin.graphql.graphql.SubscriptionGraphql
import io.javalin.plugin.graphql.server.JavalinDataLoaderRegistryFactory

class GraphQLPluginBuilder<out T : GraphQLContext>(val path: String, val contextFactory: GraphQLContextFactory<T, Context>) {
    private var queries: MutableList<TopLevelObject> = mutableListOf()
    private var mutations: MutableList<TopLevelObject> = mutableListOf()
    private var subscriptions: MutableList<TopLevelObject> = mutableListOf()
    private var packages: MutableList<String> = mutableListOf("kotlin.Unit")
    private val dataLoaders: MutableList<KotlinDataLoader<*, *>> = mutableListOf()

    companion object {
        fun create(options: GraphQLOptions): GraphQLPluginBuilder<*> {
            val graphQLPluginBuilder = GraphQLPluginBuilder(options.path, EmptyGraphQLContextFactory())
            graphQLPluginBuilder.queries = options.queries
            graphQLPluginBuilder.mutations = options.mutations
            graphQLPluginBuilder.subscriptions = options.subscriptions
            graphQLPluginBuilder.packages = options.packages
            return graphQLPluginBuilder
        }
    }

    fun add(aPackage: String) = apply { packages.add(aPackage) }

    fun register(vararg dataLoaders: KotlinDataLoader<*, *>) = apply { this.dataLoaders.addAll(dataLoaders) }

    fun register(vararg queries: QueryGraphql) = apply {
        this.queries.addAll(queries.map { TopLevelObject(it) })
    }

    fun register(vararg mutations: MutationGraphql) = apply {
        this.mutations.addAll(mutations.map { TopLevelObject(it) })
    }

    fun register(vararg subscriptions: SubscriptionGraphql) = apply {
        this.subscriptions.addAll(subscriptions.map { TopLevelObject(it) })
    }

    fun build() = GraphQLPlugin(this)

    internal fun createSchema() = GraphQL.newGraphQL(toSchema(
        config = SchemaGeneratorConfig(supportedPackages = packages),
        queries = queries,
        mutations = mutations,
        subscriptions = subscriptions
    )).build()!!

    fun toJavalinDataLoaderRegistryFactory() = JavalinDataLoaderRegistryFactory(dataLoaders)
}
