package io.javalin.vue

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.Header
import io.javalin.http.InternalServerErrorResponse
import io.javalin.json.jsonMapper
import io.javalin.vue.FileInliner.inlineFiles
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.regex.Matcher

abstract class VueHandler(private val componentId: String) : Handler {

    open fun state(ctx: Context): Any? = null
    open fun preRender(layout: String, ctx: Context): String = layout
    open fun postRender(layout: String, ctx: Context): String = layout

    override fun handle(ctx: Context) {
        val c = ctx.appAttribute<JavalinVueConfig>(JAVALINVUE_CONFIG_KEY)
        c.isDev = c.isDev ?: c.isDevFunction(ctx)
        c.rootDirectory = c.rootDirectory ?: c.pathMaster.defaultLocation(c.isDev!!)
        val routeComponent = if (componentId.startsWith("<")) componentId else "<$componentId></$componentId>"
        val allFiles = if (c.isDev == true) c.pathMaster.walkPaths() else c.pathMaster.cachedPaths
        val resolver by lazy { if (c.isDev == true) VueDependencyResolver(allFiles, c.vueAppName) else c.pathMaster.cachedDependencyResolver }
        val componentId = routeComponent.removePrefix("<").takeWhile { it !in setOf('>', ' ') }
        val dependencies = if (c.optimizeDependencies) resolver.resolve(componentId) else allFiles.joinVueFiles()
        if (componentId !in dependencies) throw InternalServerErrorResponse("Route component not found: $routeComponent")
        ctx.html(
            allFiles.find { it.endsWith("vue/layout.html") }!!.readText() // we start with the layout file
                .preRenderHook(ctx)
                .inlineFiles(c.isDev!!, allFiles.filterNot { it.isVueFile() }) // we then inline css/js files
                .replace("@componentRegistration", "@loadableData@componentRegistration@serverState") // add anchors for later
                .replace("@loadableData", loadableDataScript) // add loadable data class
                .replace("@componentRegistration", dependencies) // add all dependencies
                .replace("@serverState", getState(ctx, state(ctx))) // add escaped params and state
                .replace("@routeComponent", routeComponent) // finally, add the route component itself
                .replace("@cdnWebjar/", if (c.isDev == true) "/webjars/" else "https://cdn.jsdelivr.net/webjars/org.webjars.npm/")
                .insertNoncesAndCspHeader(c.enableCspAndNonces, ctx)
                .postRenderHook(ctx)
        ).header(Header.CACHE_CONTROL, c.cacheControl)
    }

    private fun String.preRenderHook(ctx: Context) = preRender(this, ctx);
    private fun String.postRenderHook(ctx: Context) = postRender(this, ctx);
}

private fun String.insertNoncesAndCspHeader(enableCspAndNonces: Boolean, ctx: Context): String {
    if (enableCspAndNonces) return this.replace("nonce=\"@internalAddNonce\"", "") // remove from loadabledata and state snippets
    val nonces = mutableSetOf<String>()
    fun MutableSet<String>.newNonce() = ("jv-" + UUID.randomUUID().toString().replace("-", "")).also { this.add(it) }
    return this
        .replace("@internalAddNonce".toRegex()) { nonces.newNonce() }
        .replace("@addNonce".toRegex()) { nonces.newNonce() }
        .also { ctx.header(Header.CONTENT_SECURITY_POLICY, "script-src 'unsafe-eval' ${nonces.joinToString(" ") { "'nonce-$it'" }}") }
}

object FileInliner {
    private val newlineRegex = Regex("\\r?\\n")
    private val unconditionalRegex = Regex("""@inlineFile\(".*"\)""")
    private val devRegex = Regex("""@inlineFileDev\(".*"\)""")
    private val notDevRegex = Regex("""@inlineFileNotDev\(".*"\)""")

    fun String.inlineFiles(isDev: Boolean, nonVueFiles: List<Path>): String {
        val pathMap = nonVueFiles.associateBy { """"/vue/${it.toString().replace("\\", "/").substringAfter("/vue/")}"""" } // normalize keys
        return this.split(newlineRegex).joinToString("\n") { line ->
            if (!line.contains("@inlineFile")) return@joinToString line // nothing to inline
            val matchingKey = pathMap.keys.find { line.contains(it) } ?: throw IllegalStateException("Invalid path found: $line")
            val matchingFileContent by lazy { Matcher.quoteReplacement(pathMap[matchingKey]!!.readText()) }
            when {
                devRegex.containsMatchIn(line) -> if (isDev) line.replace(devRegex, matchingFileContent) else ""
                notDevRegex.containsMatchIn(line) -> if (!isDev) line.replace(notDevRegex, matchingFileContent) else ""
                else -> line.replace(unconditionalRegex, matchingFileContent)
            }
        }
    }
}

internal fun getState(ctx: Context, state: Any?) =
    "\n<script nonce=\"@internalAddNonce\">\n${ctx.prototypeOrGlobalConfig()}.\$javalin = JSON.parse(decodeURIComponent('${urlEncodedState(ctx, state)}'))\n</script>\n"

private fun urlEncodedState(ctx: Context, state: Any?) = urlEncodeForJavascript(
    ctx.jsonMapper().toJsonString(
        mapOf(
            "pathParams" to ctx.pathParamMap(),
            "state" to (state ?: ctx.invokeStateFunction())
        )
    )
)

private fun urlEncodeForJavascript(string: String) = URLEncoder.encode(string, Charsets.UTF_8.name()).replace("+", "%20")

private fun Context.invokeStateFunction() =
    this.appAttribute<JavalinVueConfig>(JAVALINVUE_CONFIG_KEY).stateFunction(this)

private fun Context.prototypeOrGlobalConfig(): String {
    val cfg = this.appAttribute<JavalinVueConfig>(JAVALINVUE_CONFIG_KEY)
    return if (cfg.vueAppName != null) "${cfg.vueAppName}.config.globalProperties" else "Vue.prototype"
}

// Unfortunately, Java's URLEncoder does not encode the space character in the same way as Javascript.
// Javascript expects a space character to be encoded as "%20", whereas Java encodes it as "+".
// All other encodings are implemented correctly, therefore we can simply replace the character in the encoded String.
internal fun Path.readText() = String(Files.readAllBytes(this))
internal fun Path.isVueFile() = this.toString().endsWith(".vue")
private fun Set<Path>.joinVueFiles() = this.filter { it.isVueFile() }.joinToString("") { "\n<!-- ${it.fileName} -->\n" + it.readText() }
