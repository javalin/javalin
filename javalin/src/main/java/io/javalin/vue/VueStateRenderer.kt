package io.javalin.vue

import io.javalin.http.Context
import io.javalin.json.toJsonString
import org.eclipse.jetty.util.URIUtil

internal object VueStateRenderer {
    fun getState(ctx: Context, state: Any?): String {
        val cfg = ctx.appAttribute<JavalinVueConfig>(JAVALINVUE_CONFIG_KEY)
        fun urlEncodedState(state: Any?): String = ctx.jsonMapper()
            .toJsonString(
                mapOf(
                    "pathParams" to ctx.pathParamMap(),
                    "state" to (state ?: cfg.stateFunction(ctx))
                )
            )
            .urlEncodeForJavascript()

        val prototypeOrGlobalConfig = if (cfg.vueInstanceNameInJs != null) "${cfg.vueInstanceNameInJs}.config.globalProperties" else "Vue.prototype"
        return """
            <script nonce="@internalAddNonce">
                window.JavalinVue = JSON.parse(decodeURIComponent('${urlEncodedState(state)}'))
                $prototypeOrGlobalConfig.${'$'}javalin = window.JavalinVue
            </script>
        """.trimIndent()
    }

    // Unfortunately, Java's URLEncoder#encode is slightly different from JavaScript's encodeURIComponent.
    // Luckily, Jetty has a util which works exactly the same.
    private fun String.urlEncodeForJavascript() = URIUtil.encodePath(this)
}
