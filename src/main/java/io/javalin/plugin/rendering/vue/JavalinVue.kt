/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.rendering.vue

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.util.ContextUtil
import io.javalin.plugin.json.JavalinJson
import java.io.File

object JavalinVue {

    private val cachedTemplate: String by lazy { createLayout(localhost = false) }

    fun render(component: String) = Handler { ctx ->
        var view = if (ContextUtil.isLocalhost(ctx)) createLayout(localhost = true) else cachedTemplate
        view = view.replace("@routeParams", getParams(ctx)) // insert path params and query params
        ctx.html(view.replace("@routeComponent", component)) // insert current route component
    }

    private fun createLayout(localhost: Boolean): String {
        val vueFolder = if (localhost) "src/main/resources/vue" else this.javaClass.classLoader.getResource("vue").path
        val headContent = File(vueFolder).walkTopDown().filter { it.extension == "vue" }.joinToString("") { it.readText() } // // find and concat .vue files
        return File("$vueFolder/layout.html").readText().replace("@componentRegistration", "@routeParams$headContent") // inject variable for route-params
    }

    private fun getParams(ctx: Context) = """<script>
          Vue.prototype.${"$"}javalin = {
          pathParams: ${JavalinJson.toJson(ctx.pathParamMap())},
          queryParams: ${JavalinJson.toJson(ctx.queryParamMap())}
      }</script>"""

}
