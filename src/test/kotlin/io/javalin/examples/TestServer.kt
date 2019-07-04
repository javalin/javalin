package io.javalin.examples

import io.javalin.Javalin

private data class SillyObject(var fieldOne: String, var fieldTwo: String, var fieldThree: String)
private fun getSomeObjects(numberOfObjects: Int) = (1..numberOfObjects).map { i -> SillyObject("f$i", "f$i", "f$i") }.toList()

val gzip = true
val brotli = true

fun main(args: Array<String>) {
    val app = Javalin.create()
    app.config.dynamicGzip = gzip
    app.config.dynamicBrotli = brotli
    //app.config.addStaticFiles("src/test/external/", Location.EXTERNAL)
    app.get("/huge") { ctx -> ctx.result(getSomeObjects(1000).toString()) }
    app.get("/smol") { ctx -> ctx.result(getSomeObjects(10).toString()) }
    app.start(7000)
    }

