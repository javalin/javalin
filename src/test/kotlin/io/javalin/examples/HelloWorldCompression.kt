package io.javalin.examples

import io.javalin.Javalin
import io.javalin.core.compression.Brotli
import io.javalin.core.compression.CompressionStrategy
import io.javalin.core.compression.Gzip

private data class SillyObject(var fieldOne: String, var fieldTwo: String, var fieldThree: String)
private fun getSomeObjects(numberOfObjects: Int) = (1..numberOfObjects).map { i -> SillyObject("f$i", "f$i", "f$i") }.toList()

/**
 * This code is just here for testing purposes. It will be removed before PR is merged
 */
fun main(args: Array<String>) {
    val app = Javalin.create {config ->
        config.compressionStrategy( CompressionStrategy(Brotli(), Gzip()) )
        config.addStaticFiles("/public")
    }.start(7070)
    app.get("/huge") { ctx -> ctx.result(getSomeObjects(1000).toString()) }
    app.get("/medium") { ctx -> ctx.result(getSomeObjects(200).toString()) }
    app.get("/tiny") { ctx -> ctx.result(getSomeObjects(10).toString()) }
}
