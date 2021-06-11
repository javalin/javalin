package io.javalin.http.context

import io.javalin.http.Context
import io.javalin.plugin.json.JavalinJson
import java.util.concurrent.CompletableFuture

class AsyncContext(private val syncContext: Context) {

    private var result: CompletableFuture<*>? = null

    fun result() = result

    fun result(future: CompletableFuture<*>): AsyncContext {
        result = future
        return this
    }

    fun json(future: CompletableFuture<*>): AsyncContext = result(future.thenApply {
        if (it != null) JavalinJson.toJson(it).also { syncContext.contentType("application/json") } else ""
    })

}
