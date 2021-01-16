package io.javalin.examples

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import java.io.FileInputStream

fun main(args: Array<String>) {

    val app = Javalin.create().start(1337)

    app.routes {
        get("/file") { ctx ->
            ctx.writeCallback = { sent, all ->
                println("$sent $all")
            }
            ctx.seekableStream(FileInputStream("/path/to/some/big/file"), "binary")
        }
    }
}
