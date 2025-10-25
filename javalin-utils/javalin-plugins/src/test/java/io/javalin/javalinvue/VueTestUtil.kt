package io.javalin.javalinvue

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.JavalinVuePlugin
import io.javalin.testtools.JavalinTest
import io.javalin.testtools.TestCase
import io.javalin.vue.JavalinVueConfig
import java.util.function.Consumer

object VueTestUtil {
    @JvmStatic
    fun test(vueConfig: Consumer<JavalinVueConfig>?, config: Consumer<JavalinConfig>?, test: TestCase) =
        JavalinTest.test(
            app = Javalin.create { baseConfig ->
                baseConfig.registerPlugin(JavalinVuePlugin { vue ->
                    vue.rootDirectory("src/test/resources/vue", Location.EXTERNAL)
                    vueConfig?.accept(vue)
                })
                config?.accept(baseConfig)
            },
            testCase = test
        )
}

// Java-compatible alias
class VueJavalinTest {
    companion object {
        @JvmStatic
        fun test(vueConfig: Consumer<JavalinVueConfig>?, config: Consumer<JavalinConfig>?, test: TestCase) =
            VueTestUtil.test(vueConfig, config, test)
    }
}
