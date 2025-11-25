package io.javalin.javalinvue

import io.javalin.Javalin
import io.javalin.config.JavalinState
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.bundled.JavalinVuePlugin
import io.javalin.testing.HttpUtil
import io.javalin.testing.TestUtil
import io.javalin.testing.ThrowingBiConsumer
import io.javalin.vue.JavalinVueConfig
import java.util.function.Consumer

object VueTestUtil {
    @JvmStatic
    fun test(vueConfig: Consumer<JavalinVueConfig>?, config: Consumer<JavalinState>?, test: ThrowingBiConsumer<Javalin, HttpUtil>) =
        TestUtil.test(Javalin.create { baseConfig ->
            baseConfig.registerPlugin(JavalinVuePlugin { vue ->
                vue.rootDirectory("src/test/resources/vue", Location.EXTERNAL)
                vueConfig?.accept(vue)
            })
            config?.accept(baseConfig)
        }, test)
}
