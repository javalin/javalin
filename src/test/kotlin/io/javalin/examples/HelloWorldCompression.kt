package io.javalin.examples

import io.javalin.Javalin
import io.javalin.core.compression.DynamicCompressionStrategy

private data class SillyObject(var fieldOne: String, var fieldTwo: String, var fieldThree: String)
private fun getSomeObjects(numberOfObjects: Int) = (1..numberOfObjects).map { i -> SillyObject("f$i", "f$i", "f$i") }.toList()

fun main(args: Array<String>) {
    val app = Javalin.create {config ->
        config.inner.dynamicCompressionStrategy = DynamicCompressionStrategy(brotliEnabled = true, gzipEnabled = true, brotliLevel =  0, gzipLevel =  0)
    }.start(7071)
    app.get("/huge") { ctx -> ctx.result(getSomeObjects(1000).toString()) }
    app.get("/medium") { ctx -> ctx.result(getSomeObjects(200).toString()) }
    app.get("/tiny") { ctx -> ctx.result(getSomeObjects(10).toString()) }
}

