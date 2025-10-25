package io.javalin.plugin

import io.javalin.config.JavalinConfig
import io.javalin.plugin.Plugin
import io.javalin.vue.JavalinVueConfig
import io.javalin.vue.JavalinVueConfig.Companion.VueConfigKey
import java.util.function.Consumer

/**
 * Plugin for JavalinVue support.
 * Enables server-side rendering of Vue components with state management.
 * 
 * @see [JavalinVueConfig]
 * @see [Online Doc](https://javalin.io/plugins/javalinvue)
 */
class JavalinVuePlugin(userConfig: Consumer<JavalinVueConfig>? = null) : Plugin<JavalinVueConfig>(userConfig, JavalinVueConfig()) {
    
    override fun onStart(config: JavalinConfig) {
        config.appData(VueConfigKey, pluginConfig)
    }
    
}

