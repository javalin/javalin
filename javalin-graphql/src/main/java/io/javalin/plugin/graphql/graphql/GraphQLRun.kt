package io.javalin.plugin.graphql.graphql

import graphql.ExceptionWhileDataFetching
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.GraphQL
import org.eclipse.jetty.server.handler.ContextHandler
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import java.util.concurrent.CompletableFuture

class GraphQLRun(private val graphql: GraphQL) {
    private var context: Any? = null
    private var query: String = ""
    private var variables: Map<String, Any> = emptyMap()

    fun withQuery(query: String): GraphQLRun = apply { this.query = query }

    fun withVariables(variables: Map<String, Any>): GraphQLRun = apply { this.variables = variables }

    fun withContext(context: Any?): GraphQLRun = apply { this.context = context }

    fun execute(): CompletableFuture<MutableMap<String, Any>> {
        val action = generateAction();
        return graphql.executeAsync(action)
                .thenApplyAsync { this.getResult(it) }
    }


    private fun generateAction(): ExecutionInput {
        return ExecutionInput.newExecutionInput()
                .variables(variables)
                .query(query)
                .context(context)
                .build()
    }


    private fun getResult(executionResult: ExecutionResult): MutableMap<String, Any> {
        val result = mutableMapOf<String, Any>()

        if (executionResult.errors.isNotEmpty()) {
            result["errors"] = executionResult.errors.distinctBy {
                if (it is ExceptionWhileDataFetching) {
                    it.exception
                } else {
                    it
                }
            }
        }

        try {
            result["data"] = executionResult.getData<Any>()
        } catch (e: Exception) {}

        return result
    }

    fun subscribe(subscriber: Subscriber<ExecutionResult>): Unit {
        val action = generateAction()
        return graphql
                .execute(action)
                .getData<Publisher<ExecutionResult>>()
                .subscribe(subscriber)
    }
}
