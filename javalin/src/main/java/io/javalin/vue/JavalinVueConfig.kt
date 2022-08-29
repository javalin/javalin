package io.javalin.vue

import io.javalin.http.Context
import io.javalin.http.servlet.isLocalhost
import io.javalin.http.staticfiles.Location
import java.nio.file.Path
import java.nio.file.Paths

const val JAVALINVUE_CONFIG_KEY = "javalin-javalinvue-config"

class JavalinVueConfig {
    //@formatter:off
    @get:JvmSynthetic @set:JvmSynthetic internal var pathMaster = VuePathMaster(this)
    @get:JvmSynthetic @set:JvmSynthetic internal var rootDirectory: Path? = null // is set on first request (if not configured)
    fun rootDirectory(path: Path) {
        this.rootDirectory = path
    }
    @JvmOverloads fun rootDirectory(path: String, location: Location = Location.CLASSPATH, resourcesJarClass: Class<*> = VuePathMaster::class.java) {
        this.rootDirectory = if (location == Location.CLASSPATH) pathMaster.classpathPath(path, resourcesJarClass) else Paths.get(path)
    }

    @JvmField var vueAppName: String? = null // only relevant for Vue 3 apps

    @get:JvmSynthetic @set:JvmSynthetic internal var isDev: Boolean? = null // cached and easily accessible, is set on first request (can't be configured directly by end user)
    @JvmField var isDevFunction: (Context) -> Boolean = { it.isLocalhost() } // used to set isDev, will be called once

    @JvmField var optimizeDependencies = true // only include required components for the route component

    @JvmField var stateFunction: (Context) -> Any = { mapOf<String, String>() } // global state that is injected into all VueComponents

    @JvmField var cacheControl = "no-cache, no-store, must-revalidate"

    @JvmField var enableCspAndNonces = false
    //@formatter:on
}
