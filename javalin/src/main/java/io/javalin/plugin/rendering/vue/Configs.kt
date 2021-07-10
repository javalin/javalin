package io.javalin.plugin.rendering.vue

import java.nio.file.Path
import java.nio.file.Paths

class VueDirConfig {
    fun externalPath(path: String) {
        JavalinVue.rootDirectory = Paths.get(path)
    }

    @JvmOverloads
    fun classpathPath(path: String, resourcesJarClass: Class<*> = PathMaster::class.java) {
        JavalinVue.resourcesJarClass = resourcesJarClass // used by line below (global import...)
        JavalinVue.rootDirectory = PathMaster.classpathPath(path)
    }

    fun explicitPath(path: Path) {
        JavalinVue.rootDirectory = path
    }
}

class VueVersionConfig {
    fun vue2() {
        JavalinVue.vueVersion = VueVersion.VUE_2
        JavalinVue.vueAppName = "Vue"
    }

    fun vue3(appName: String) {
        JavalinVue.vueVersion = VueVersion.VUE_3
        JavalinVue.vueAppName = appName
    }
}
