/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.rendering.vue

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.util.ContextUtil.isLocalhost
import io.javalin.plugin.json.JavalinJson
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors

object JavalinVue {

    var localPath = "src/main/resources/vue"
    var paths = setOf<Path>()
    var stateFunction: (Context) -> Any = { mapOf<String, String>() }

    val cachedLayout by lazy { createLayout() }
    val cachedPaths by lazy { walkPaths() }
    val vueRootUri by lazy { JavalinVue::class.java.getResource("/vue").toURI() }
    val vueRootPath by lazy { if (vueRootUri.scheme == "jar") fileSystem.getPath("/vue") else Paths.get(localPath) }
    val fileSystem by lazy { FileSystems.newFileSystem(vueRootUri, emptyMap<String, Any>()) }

    fun createLayout() = layout().replace("@componentRegistration", "@serverState${components()}") // add state anchor for later
    fun layout() = paths.find { it.endsWith("vue/layout.html") }!!.readText()
    fun components() = paths.filter { it.toString().endsWith(".vue") }.joinToString("") { it.readText() }
    fun walkPaths() = Files.walk(vueRootPath, 10).collect(Collectors.toSet())

    fun getState(ctx: Context, state: Any) = "\n<script>\n" + """
        |    Vue.prototype.${"$"}javalin = {
        |        pathParams: ${JavalinJson.toJson(ctx.pathParamMap())},
        |        queryParams: ${JavalinJson.toJson(ctx.queryParamMap())},
        |        state: ${JavalinJson.toJson(state)}
        |    }""".trimMargin() + "\n</script>\n"

    private fun Path.readText(): String {
        val s = Scanner(Files.newInputStream(this)).useDelimiter("\\A")
        return if (s.hasNext()) s.next() else ""
    }

}

class VueComponent(val component: String) : Handler {
    override fun handle(ctx: Context) {
        JavalinVue.paths = if (ctx.isLocalhost()) JavalinVue.walkPaths() else JavalinVue.cachedPaths
        val view = if (ctx.isLocalhost()) JavalinVue.createLayout() else JavalinVue.cachedLayout
        val state = JavalinVue.getState(ctx, JavalinVue.stateFunction.invoke(ctx))
        ctx.header("Cache-Control", "no-cache, no-store, must-revalidate")
        ctx.html(view.replace("@serverState", state).replace("@routeComponent", component)) // insert current route component
    }
}
