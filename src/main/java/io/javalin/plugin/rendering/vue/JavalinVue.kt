/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.rendering.vue

import io.javalin.core.util.Header
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.staticfiles.Location
import io.javalin.http.util.ContextUtil.isLocalhost
import io.javalin.plugin.json.JavalinJson
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

object JavalinVue {

    private var vueDirPath: Path? = null

    @JvmStatic
    fun rootDirectory(path: String, location: Location) {
        vueDirPath = if (location == Location.CLASSPATH) PathMaster.classpathPath(path) else Paths.get(path)
    }

    @JvmField
    var stateFunction: (Context) -> Any = { mapOf<String, String>() }

    internal fun walkPaths(): Set<Path> = Files.walk(vueDirPath, 10).collect(Collectors.toSet())

    internal val cachedPaths by lazy { walkPaths() }
    internal val cachedLayout by lazy { createLayout(cachedPaths) }

    internal fun createLayout(paths: Set<Path>) = paths
            .find { it.endsWith("vue/layout.html") }!!.readText()
            .replace("@componentRegistration", "@componentRegistration@serverState") // add state anchor for later
            .replace("@componentRegistration", paths
                    .filter { it.toString().endsWith(".vue") }
                    .joinToString("") { "\n<!-- ${it.fileName} -->\n" + it.readText() }
            )

    internal fun getState(ctx: Context) = "\n<script>\n" + """
        |    Vue.prototype.${"$"}javalin = {
        |        pathParams: ${JavalinJson.toJson(ctx.pathParamMap())},
        |        queryParams: ${JavalinJson.toJson(ctx.queryParamMap())},
        |        state: ${JavalinJson.toJson(stateFunction(ctx))}
        |    }""".trimMargin() + "\n</script>\n"

    internal fun setRootDirPathIfUnset(ctx: Context) {
        vueDirPath = vueDirPath ?: if (ctx.isLocalhost()) Paths.get("src/main/resources/vue") else PathMaster.classpathPath("/vue")
    }

}

class VueComponent(private val component: String) : Handler {
    override fun handle(ctx: Context) {
        JavalinVue.setRootDirPathIfUnset(ctx)
        val routeComponent = if (component.startsWith("<")) component else "<$component></$component>"
        val paths = if (ctx.isLocalhost()) JavalinVue.walkPaths() else JavalinVue.cachedPaths
        val view = if (ctx.isLocalhost()) JavalinVue.createLayout(paths) else JavalinVue.cachedLayout
        val componentName = routeComponent.removePrefix("<").takeWhile { it !in setOf('>', ' ') }
        if (!view.contains(componentName)) {
            ctx.result("Route component not found: $routeComponent")
            return
        }
        ctx.header(Header.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
        ctx.html(view.replace("@serverState", JavalinVue.getState(ctx)).replace("@routeComponent", routeComponent)) // insert current route component
    }
}

object PathMaster {
    private val fileSystem by lazy { FileSystems.newFileSystem(PathMaster::class.java.getResource("").toURI(), emptyMap<String, Any>()) }
    fun classpathPath(path: String): Path = when {
        PathMaster::class.java.getResource(path).toURI().scheme == "jar" -> fileSystem.getPath(path)
        else -> Paths.get(PathMaster::class.java.getResource(path).toURI())
    }
}

fun Path.readText() = String(Files.readAllBytes(this))
