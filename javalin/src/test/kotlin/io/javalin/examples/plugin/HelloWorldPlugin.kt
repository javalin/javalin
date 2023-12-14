package io.javalin.examples.plugin

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Bucket4j
import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.TooManyRequestsResponse
import io.javalin.plugin.ContextPlugin
import java.time.Duration
import java.util.function.Consumer

fun main() {
    val app = Javalin.create {
        it.registerPlugin(Bucket4K {
            it.limit = Bandwidth.simple(10, Duration.ofMinutes(1))
        })
    }
    app.get("/cheap-endpoint") { ctx: Context ->
        ctx.with(Bucket4K::class).tryConsume(1)
        ctx.result("Hello, you've accessed the cheap endpoint!")
    }
    app.get("/expensive-endpoint") { ctx: Context ->
        ctx.with(Bucket4K::class).tryConsume(5)
        ctx.result("Hello, you've accessed the expensive endpoint!")
    }
    app.start(7000)
}

class Bucket4K(userConfig: Consumer<Config>) : ContextPlugin<Bucket4K.Config, Bucket4K.Extension>(userConfig, Config()) {
    // any sort of properties your plugin needs should be defined here
    private val buckets: MutableMap<String, Bucket> = HashMap()
    // will be called when the user accesses the plugin through ctx.with(Bucket4K::class)
    override fun createExtension(context: Context) = Extension(context)
    // will be made available to the user in registerPlugin(Bucket4K {...})
    class Config {
        var limit = Bandwidth.simple(10, Duration.ofMinutes(1))
    }
    // will be available through ctx.with(Bucket4K::class)
    inner class Extension(val ctx: Context) {
        fun tryConsume(tokens: Int) {
            val ip = ctx.ip()
            val bucket = buckets.computeIfAbsent(ip) { k: String? ->
                Bucket4j.builder().addLimit(pluginConfig.limit).build()
            }
            val consumed = bucket.tryConsume(tokens.toLong())
            if (!consumed) {
                throw TooManyRequestsResponse()
            }
        }
    }
}
