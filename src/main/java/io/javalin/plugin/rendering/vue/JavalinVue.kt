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

    val cachedLayout by lazy { createLayout() }
    val cachedPaths by lazy { walkPaths() }

    fun createLayout() = layout().replace("@componentRegistration", "@routeParams${components()}") // add params anchor for later
    private fun layout() = paths.find { it.endsWith("vue/layout.html") }!!.readText()
    private fun components() = paths.filter { it.toString().endsWith(".vue") }.joinToString("") { it.readText() }

    fun getParams(ctx: Context) = """<script>
            Vue.prototype.${"$"}javalin = {
            pathParams: ${JavalinJson.toJson(ctx.pathParamMap())},
            queryParams: ${JavalinJson.toJson(ctx.queryParamMap())}
        }</script>"""

    fun walkPaths(): Set<Path> {
        val uri = JavalinVue::class.java.getResource("/vue").toURI()
        val path = if (uri.scheme == "jar") {
            val fileSystem = FileSystems.newFileSystem(uri, emptyMap<String, Any>())
            fileSystem.getPath("/vue")
        } else {
            Paths.get(localPath)
        }
        return Files.walk(path, 10).collect(Collectors.toSet())
    }

    private fun Path.readText(): String {
        val s = Scanner(Files.newInputStream(this)).useDelimiter("\\A")
        return if (s.hasNext()) s.next() else ""
    }

}

class VueComponent(private val component: String) : Handler {
    override fun handle(ctx: Context) {
        JavalinVue.paths = if (ctx.isLocalhost()) JavalinVue.walkPaths() else JavalinVue.cachedPaths
        val view = if (ctx.isLocalhost()) JavalinVue.createLayout() else JavalinVue.cachedLayout
        ctx.html(view.replace("@routeParams", JavalinVue.getParams(ctx)).replace("@routeComponent", component)) // insert current route component
    }
}
