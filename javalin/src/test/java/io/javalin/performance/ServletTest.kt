package io.javalin.performance

import io.javalin.Javalin
import kong.unirest.Unirest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch

class Test {

    //@Test
    fun test() {
        val await = CountDownLatch(1)
        var id = 0

        val app = Javalin.create()
            .before("/raw") { it.result("Before")}
            .get("/raw") {
                it.result("Http")
                println("Request ${id++}")
            }
            .after("/raw") { it.result("After") }
            .get("/stop") { await.countDown() }
            .start(8080)

        await.await()
    }

}

fun main() {
    repeat(8) {
        CompletableFuture.runAsync {
            while (true) {
                Unirest.get("http://localhost:8080/raw")
                    .asString()
                    .body
            }
        }
    }

    Thread.sleep(Long.MAX_VALUE)
}

