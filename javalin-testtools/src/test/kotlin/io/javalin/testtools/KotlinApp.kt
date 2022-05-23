package io.javalin.testtools

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import io.javalin.http.Context
import io.javalin.http.sse.SseClient

// We're using objects for simplicity's sake, but you could
// make it classes and do dependency injection or whatever
object KotlinApp {
    var app = Javalin.create { javalin ->
        javalin.ignoreTrailingSlashes = false
    }.routes {
        ApiBuilder.get("/hello", HelloController::hello)
        ApiBuilder.sse("/listen", HelloController::sayHelloFiveTimes)
    }

    internal object HelloController {
        fun hello(ctx: Context) {
            ctx.result("Hello, app!")
        }

        fun sayHelloFiveTimes(sseClient: SseClient) {
            repeat(5) {
                sseClient.sendEvent("Hello!")
                Thread.sleep(200)
            }
            sseClient.close()
        }
    }
}
