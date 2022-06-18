package io.javalin.testtools

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import io.javalin.http.Context

// We're using objects for simplicity's sake, but you could
// make it classes and do dependency injection or whatever
object KotlinApp {
    var app = Javalin.create { javalin ->
        javalin.routing.ignoreTrailingSlashes = false
    }.routes {
        ApiBuilder.get("/hello", HelloController::hello)
    }

    internal object HelloController {
        fun hello(ctx: Context) {
            ctx.result("Hello, app!")
        }
    }
}
