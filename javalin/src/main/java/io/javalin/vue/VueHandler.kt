package io.javalin.vue

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.Header
import io.javalin.http.InternalServerErrorResponse
import io.javalin.util.javalinLazy
import io.javalin.vue.JavalinVueConfig.Companion.VueConfigKey
import io.javalin.vue.VueFileInliner.inlineFiles
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

abstract class VueHandler(private val componentId: String) : Handler {

    open fun state(ctx: Context): Any? = null
    open fun preRender(layout: String, ctx: Context): String = layout
    open fun postRender(layout: String, ctx: Context): String = layout

    override fun handle(ctx: Context) {
        val c = ctx.appData(VueConfigKey)
        c.isDev = c.isDev ?: c.isDevFunction(ctx)
        c.rootDirectory = c.rootDirectory ?: c.pathMaster.defaultLocation(c.isDev!!)
        val routeComponent = if (componentId.startsWith("<")) componentId else "<$componentId></$componentId>"
        val allFiles = if (c.isDev == true) c.pathMaster.walkPaths() else c.pathMaster.cachedPaths
        val resolver by javalinLazy { if (c.isDev == true) VueDependencyResolver(allFiles, c.vueInstanceNameInJs) else c.pathMaster.cachedDependencyResolver }
        val componentId = routeComponent.removePrefix("<").takeWhile { it !in setOf('>', ' ') }
        val dependencies = if (c.optimizeDependencies) resolver.resolve(componentId) else allFiles.joinVueFiles()
        if (componentId !in dependencies) throw InternalServerErrorResponse("Route component not found: $routeComponent")
        ctx.html(
            allFiles.find { it.endsWith("vue/layout.html") }!!.readText() // we start with the layout file
                .preRenderHook(ctx)
                .inlineFiles(c.isDev!!, allFiles.filterNot { it.isVueFile() }) // we then inline css/js files
                .replace("@componentRegistration", "@serverState@loadableData@componentRegistration") // add anchors for later
                .replace("@serverState", VueStateRenderer.getState(ctx, state(ctx))) // add escaped params and state
                .replace("@loadableData", if (c.enableLoadableData) loadableDataScript else "") // add loadable data class (disabled by default)
                .replace("@componentRegistration", dependencies) // add all dependencies
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
    if (!enableCspAndNonces) return this.replace("nonce=\"@internalAddNonce\"", "") // remove from loadabledata and state snippets
    val nonces = mutableSetOf<String>()
    fun MutableSet<String>.newNonce() = ("jv-" + UUID.randomUUID().toString().replace("-", "")).also { this.add(it) }
    return this
        .replace("@internalAddNonce".toRegex()) { nonces.newNonce() }
        .replace("@addNonce".toRegex()) { nonces.newNonce() }
        .also { ctx.header(Header.CONTENT_SECURITY_POLICY, "script-src 'unsafe-eval' ${nonces.joinToString(" ") { "'nonce-$it'" }}") }
}

internal fun Path.readText() = String(Files.readAllBytes(this))
internal fun Path.isVueFile() = this.toString().endsWith(".vue")
private fun Set<Path>.joinVueFiles() = this.filter { it.isVueFile() }.joinToString("") { "\n<!-- ${it.fileName} -->\n" + it.readText() }
