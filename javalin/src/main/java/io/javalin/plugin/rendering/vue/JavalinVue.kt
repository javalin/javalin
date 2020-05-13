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

    internal var useCdn = false

    private var vueDirPath: Path? = null

    @JvmStatic
    fun rootDirectory(path: String, location: Location) {
        vueDirPath = if (location == Location.CLASSPATH) PathMaster.classpathPath(path) else Paths.get(path)
    }

    @JvmStatic
    fun rootDirectory(path: Path) {
        vueDirPath = path
    }

    @JvmField
    var stateFunction: (Context) -> Any = { mapOf<String, String>() }

    @JvmField
    var cacheControl = "no-cache, no-store, must-revalidate"

    internal fun walkPaths(): Set<Path> = Files.walk(vueDirPath, 10).collect(Collectors.toSet())

    internal val cachedPaths by lazy { walkPaths() }
    internal val cachedLayout by lazy { createLayout(cachedPaths) }

    internal fun createLayout(paths: Set<Path>) = paths
            .find { it.endsWith("vue/layout.html") }!!.readText()
            .replace("@componentRegistration", "@componentRegistration@serverState") // add state anchor for later
            .replace("@componentRegistration", paths
                    .filter { it.toString().endsWith(".vue") }
                    .joinToString("") { "\n<!-- ${it.fileName} -->\n" + it.readText() }
            ).replaceWebjarsWithCdn()

    internal fun getState(ctx: Context) = "\n<script>\n" + """
        |    Vue.prototype.${"$"}javalin = {
        |        pathParams: ${JavalinJson.toJson(ctx.pathParamMap())},
        |        queryParams: ${JavalinJson.toJson(ctx.queryParamMap())},
        |        state: ${JavalinJson.toJson(stateFunction(ctx))}
        |    }""".trimMargin() + "\n</script>\n"

    internal fun setRootDirPathIfUnset(ctx: Context) {
        vueDirPath = vueDirPath ?: if (ctx.isLocalhost()) Paths.get("src/main/resources/vue") else PathMaster.classpathPath("/vue")
    }

    private fun String.replaceWebjarsWithCdn() =
            this.replace("@cdnWebjar/", if (useCdn) "//cdn.jsdelivr.net/webjars/org.webjars.npm/" else "/webjars/")

}

class VueComponent(private val component: String) : Handler {
    override fun handle(ctx: Context) {
        JavalinVue.setRootDirPathIfUnset(ctx)
        JavalinVue.useCdn = !ctx.isLocalhost()
        val routeComponent = if (component.startsWith("<")) component else "<$component></$component>"
        val paths = if (ctx.isLocalhost()) JavalinVue.walkPaths() else JavalinVue.cachedPaths
        val view = if (ctx.isLocalhost()) JavalinVue.createLayout(paths) else JavalinVue.cachedLayout
        val componentName = routeComponent.removePrefix("<").takeWhile { it !in setOf('>', ' ') }
        if (!view.contains(componentName)) {
            ctx.result("Route component not found: $routeComponent")
            return
        }
        ctx.header(Header.CACHE_CONTROL, JavalinVue.cacheControl)
        ctx.html(view.replace("@serverState", JavalinVue.getState(ctx)).replace("@routeComponent", routeComponent)) // insert current route component
    }
}

object PathMaster {
    /**
     * PathMaster::class.java.getResource("").toURI() means that this code will
     * only work if the resources are in the same jar as Javalin (i.e. in a fat-jar/uber-jar).
     *
     * Creating a filesystem is required since we want to "walk" the jar (see [JavalinVue.walkPaths])
     * to find all the .vue files.
     */
    private val fileSystem by lazy { FileSystems.newFileSystem(PathMaster::class.java.getResource("").toURI(), emptyMap<String, Any>()) }

    fun classpathPath(path: String): Path = when {
        PathMaster::class.java.getResource(path).toURI().scheme == "jar" -> fileSystem.getPath(path) // we're inside a jar
        else -> Paths.get(PathMaster::class.java.getResource(path).toURI()) // we're not in jar (probably running from IDE)
    }
}

fun Path.readText() = String(Files.readAllBytes(this))
