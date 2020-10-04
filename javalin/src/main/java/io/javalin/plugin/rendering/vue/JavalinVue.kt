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
import io.javalin.plugin.rendering.vue.FileInliner.inlineFiles
import io.javalin.plugin.rendering.vue.JavalinVue.getAllDependencies
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.stream.Collectors

object JavalinVue {

    internal var isDev: Boolean? = null
    internal var vueDirPath: Path? = null

    @JvmStatic
    fun rootDirectory(path: String, location: Location) {
        vueDirPath = if (location == Location.CLASSPATH) PathMaster.classpathPath(path) else Paths.get(path)
    }

    @JvmStatic
    fun rootDirectory(path: Path) {
        vueDirPath = path
    }

    @JvmField
    var optimizeDependencies = false

    @JvmField
    var stateFunction: (Context) -> Any = { mapOf<String, String>() }

    @JvmField
    var cacheControl = "no-cache, no-store, must-revalidate"

    @JvmField
    var isDevFunction: (Context) -> Boolean = { it.isLocalhost() }

    internal fun walkPaths(): Set<Path> = Files.walk(vueDirPath, 10).collect(Collectors.toSet())

    internal val cachedPaths by lazy { walkPaths() }
    internal val cachedDependencyResolver by lazy { VueDependencyResolver(cachedPaths) }

    internal fun createLayout(paths: Set<Path>, componentDependencies: String): String {
        return paths.find { it.endsWith("vue/layout.html") }!!.readText()
                .inlineFiles(paths)
                .replace("@componentRegistration", "@componentRegistration@serverState") // add state anchor for later
                .replace("@componentRegistration", componentDependencies)
                .replaceWebjarsWithCdn()
    }

    internal fun getAllDependencies(paths: Set<Path>) = paths.filter { it.isVueFile() }
            .joinToString("") { "\n<!-- ${it.fileName} -->\n" + it.readText() }

    internal fun getState(ctx: Context, state: Any?) = "\n<script>\n" + """
        |    Vue.prototype.${"$"}javalin = {
        |        pathParams: ${JavalinJson.toJson(ctx.pathParamMap().mapKeys { escape(it.key) }.mapValues { escape(it.value) })},
        |        queryParams: ${JavalinJson.toJson(ctx.queryParamMap().mapKeys { escape(it.key) }.mapValues { it.value.map { escape(it) } })},
        |        state: ${JavalinJson.toJson(state ?: stateFunction(ctx))}
        |    }""".trimMargin() + "\n</script>\n"

    private fun String.replaceWebjarsWithCdn() =
            this.replace("@cdnWebjar/", if (isDev == true) "/webjars/" else "https://cdn.jsdelivr.net/webjars/org.webjars.npm/")

}


class VueComponent @JvmOverloads constructor(private val component: String, private val state: Any? = null) : Handler {
    override fun handle(ctx: Context) {
        JavalinVue.isDev = JavalinVue.isDev ?: JavalinVue.isDevFunction(ctx)
        JavalinVue.vueDirPath = JavalinVue.vueDirPath ?: PathMaster.defaultLocation(JavalinVue.isDev)
        val routeComponent = if (component.startsWith("<")) component else "<$component></$component>"
        val paths = if (JavalinVue.isDev == true) JavalinVue.walkPaths() else JavalinVue.cachedPaths
        val componentId = routeComponent.removePrefix("<").takeWhile { it !in setOf('>', ' ') }
        val dependencyResolver by lazy { if (JavalinVue.isDev == true) VueDependencyResolver(paths) else JavalinVue.cachedDependencyResolver }
        val view = JavalinVue.createLayout(paths, if (JavalinVue.optimizeDependencies) dependencyResolver.resolve(componentId) else getAllDependencies(paths))
        if (!view.contains(componentId)) {
            ctx.result("Route component not found: $routeComponent")
            return
        }
        ctx.header(Header.CACHE_CONTROL, JavalinVue.cacheControl)
        ctx.html(view.replace("@serverState", JavalinVue.getState(ctx, state)).replace("@routeComponent", routeComponent)) // insert current route component
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

    fun defaultLocation(isDev: Boolean?) = if (isDev == true) Paths.get("src/main/resources/vue") else classpathPath("/vue")
}

object FileInliner {
    private val newlineRegex = Regex("\\r?\\n")
    private val unconditionalRegex = Regex("""@inlineFile\(".*"\)""")
    private val devRegex = Regex("""@inlineFileDev\(".*"\)""")
    private val notDevRegex = Regex("""@inlineFileNotDev\(".*"\)""")

    fun String.inlineFiles(paths: Set<Path>): String {
        val pathMap = paths.filterNot { it.isVueFile() } // vue files are inlined in @componentRegistration later
                .associateBy { """"/vue/${it.toString().replace("\\", "/").substringAfter("/vue/")}"""" } // normalize keys
        return this.split(newlineRegex).joinToString("\n") { line ->
            if (!line.contains("@inlineFile")) return@joinToString line // nothing to inline
            val matchingKey = pathMap.keys.find { line.contains(it) } ?: throw IllegalStateException("Invalid path found: $line")
            val matchingFileContent by lazy { Matcher.quoteReplacement(pathMap[matchingKey]!!.readText()) }
            when {
                devRegex.containsMatchIn(line) -> if (JavalinVue.isDev == true) line.replace(devRegex, matchingFileContent) else ""
                notDevRegex.containsMatchIn(line) -> if (JavalinVue.isDev == false) line.replace(notDevRegex, matchingFileContent) else ""
                else -> line.replace(unconditionalRegex, matchingFileContent)
            }
        }
    }
}

fun Path.readText() = String(Files.readAllBytes(this))
fun Path.isVueFile() = this.toString().endsWith(".vue")

fun escape(string: String?) = string?.toCharArray()?.map {
    when (it) {
        '<' -> "&lt;"
        '>' -> "&gt;"
        '&' -> "&amp;"
        '"' -> "&quot;"
        '\'' -> "&#x27;"
        '/' -> "&#x2F;"
        else -> it
    }
}?.joinToString("")
