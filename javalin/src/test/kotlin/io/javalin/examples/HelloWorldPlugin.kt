package io.javalin.examples

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.TooManyRequestsResponse
import io.javalin.plugin.ContextPlugin
import java.util.function.Consumer

fun main() {
    val app = Javalin.create {
        it.registerPlugin(KRate { it.limit = 3 })
    }
    app.get("/") { ctx ->
        ctx.with(KRate::class).tryConsume(cost = 5) // will throw because limit is 3
        ctx.result("Hello World")
    }
}

// this class demonstrates the most advanced use case of a plugin,
// where the plugin has a config and a plugin extension
// we recommend using inner classes for plugins, as it keeps the whole plugin in one place
class KRate(userConfig: Consumer<Config>) : ContextPlugin<KRate.Config, KRate.Extension>(userConfig, Config()) {
    val ipToCounter = mutableMapOf<String, Int>()
    override fun createExtension(context: Context) = Extension(context)
    class Config(var limit: Int = 0)
    inner class Extension(var context: Context) {
        fun tryConsume(cost: Int = 1) {
            val ip = context.ip()
            val counter = ipToCounter.compute(ip) { _, v -> v?.plus(cost) ?: cost }!!
            if (counter > pluginConfig.limit) {
                throw TooManyRequestsResponse()
            }
        }
    }
}
