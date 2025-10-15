package io.javalin.javalinvue

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.http.staticfiles.Location
import io.javalin.testing.HttpUtil
import io.javalin.testing.TestUtil
import io.javalin.testing.*
import io.javalin.testing.ThrowingBiConsumer
import java.util.function.Consumer

object VueTestUtil {
    @JvmOverloads
    @JvmStatic
    fun test(config: Consumer<JavalinConfig>? = null, test: ThrowingBiConsumer<Javalin, HttpUtil>) =
        TestUtil.test(Javalin.create { baseConfig ->
            baseConfig.vue.rootDirectory("src/test/resources/vue", Location.EXTERNAL)
            config?.accept(baseConfig)
        }, test)
}
