package io.javalin.plugin.rendering.vue

import io.javalin.core.util.Header
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.InternalServerErrorResponse
import io.javalin.plugin.json.JavalinJson
import io.javalin.plugin.rendering.vue.FileInliner.inlineFiles
import io.javalin.plugin.rendering.vue.JavalinVue.cacheControl
import io.javalin.plugin.rendering.vue.JavalinVue.cachedDependencyResolver
import io.javalin.plugin.rendering.vue.JavalinVue.cachedPaths
import io.javalin.plugin.rendering.vue.JavalinVue.isDev
import io.javalin.plugin.rendering.vue.JavalinVue.isDevFunction
import io.javalin.plugin.rendering.vue.JavalinVue.optimizeDependencies
import io.javalin.plugin.rendering.vue.JavalinVue.rootDirectory
import io.javalin.plugin.rendering.vue.JavalinVue.stateFunction
import io.javalin.plugin.rendering.vue.JavalinVue.walkPaths
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Matcher

class VueComponent @JvmOverloads constructor(val component: String, val state: Any? = null) : Handler {
    override fun handle(ctx: Context) {
        isDev = isDev ?: isDevFunction(ctx)
        rootDirectory = rootDirectory ?: PathMaster.defaultLocation(isDev)
        val routeComponent = if (component.startsWith("<")) component else "<$component></$component>"
        val allFiles = if (isDev == true) walkPaths() else cachedPaths
        val resolver by lazy { if (isDev == true) VueDependencyResolver(allFiles, JavalinVue.vueAppName) else cachedDependencyResolver }
        val componentId = routeComponent.removePrefix("<").takeWhile { it !in setOf('>', ' ') }
        val dependencies = if (optimizeDependencies) resolver.resolve(componentId) else allFiles.joinVueFiles()
        if (componentId !in dependencies) throw InternalServerErrorResponse("Route component not found: $routeComponent")
        ctx.html(allFiles.find { it.endsWith("vue/layout.html") }!!.readText() // we start with the layout file
                .inlineFiles(allFiles.filterNot { it.isVueFile() }) // we then inline css/js files
                .replace("@componentRegistration", "@componentRegistration@serverState") // add @serverState anchor for later
                .replace("@componentRegistration", dependencies) // add all dependencies
                .replace("@serverState", getState(ctx, state)) // add the way too complex state
                .replace("@routeComponent", routeComponent) // finally, add the route component itself
                .replace("@cdnWebjar/", if (isDev == true) "/webjars/" else "https://cdn.jsdelivr.net/webjars/org.webjars.npm/")
        ).header(Header.CACHE_CONTROL, cacheControl)
    }
}

private fun Set<Path>.joinVueFiles() = this.filter { it.isVueFile() }.joinToString("") { "\n<!-- ${it.fileName} -->\n" + it.readText() }

object FileInliner {
    private val newlineRegex = Regex("\\r?\\n")
    private val unconditionalRegex = Regex("""@inlineFile\(".*"\)""")
    private val devRegex = Regex("""@inlineFileDev\(".*"\)""")
    private val notDevRegex = Regex("""@inlineFileNotDev\(".*"\)""")

    fun String.inlineFiles(nonVueFiles: List<Path>): String {
        val pathMap = nonVueFiles.associateBy { """"/vue/${it.toString().replace("\\", "/").substringAfter("/vue/")}"""" } // normalize keys
        return this.split(newlineRegex).joinToString("\n") { line ->
            if (!line.contains("@inlineFile")) return@joinToString line // nothing to inline
            val matchingKey = pathMap.keys.find { line.contains(it) } ?: throw IllegalStateException("Invalid path found: $line")
            val matchingFileContent by lazy { Matcher.quoteReplacement(pathMap[matchingKey]!!.readText()) }
            when {
                devRegex.containsMatchIn(line) -> if (isDev == true) line.replace(devRegex, matchingFileContent) else ""
                notDevRegex.containsMatchIn(line) -> if (isDev == false) line.replace(notDevRegex, matchingFileContent) else ""
                else -> line.replace(unconditionalRegex, matchingFileContent)
            }
        }
    }
}

internal fun getState(ctx: Context, state: Any?) = "\n<script>\n" +
        "${prototypeOrGlobalConfig()}.\$javalin = JSON.parse(decodeURIComponent(\"${
            urlEncodeForJavascript(JavalinJson.toJson(
                    mapOf(
                            "pathParams" to ctx.pathParamMap(),
                            "queryParams" to ctx.queryParamMap(),
                            "state" to (state ?: stateFunction(ctx))
                    )
            ))
        }\"))\n</script>\n"

// Unfortunately, Java's URLEncoder does not encode the space character in the same way as Javascript.
// Javascript expects a space character to be encoded as "%20", whereas Java encodes it as "+".
// All other encodings are implemented correctly, therefore we can simply replace the character in the encoded String.
private fun urlEncodeForJavascript(string: String) = URLEncoder.encode(string, Charsets.UTF_8.name()).replace("+", "%20")
private fun prototypeOrGlobalConfig() = if (JavalinVue.vueVersion == VueVersion.VUE_3) "${JavalinVue.vueAppName}.config.globalProperties" else "${JavalinVue.vueAppName}.prototype"
internal fun Path.readText() = String(Files.readAllBytes(this))
internal fun Path.isVueFile() = this.toString().endsWith(".vue")
