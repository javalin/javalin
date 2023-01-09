package io.javalin.vue

import io.javalin.http.Context
import io.javalin.json.jsonMapper
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

        val prototypeOrGlobalConfig = if (cfg.vueAppName != null) "${cfg.vueAppName}.config.globalProperties" else "Vue.prototype"
        return """
            <script nonce="@internalAddNonce">
                $prototypeOrGlobalConfig.${'$'}javalin = JSON.parse(decodeURIComponent('${urlEncodedState(state)}'))
            </script>
        """.trimIndent()
    }

    // Unfortunately, Java's URLEncoder does not encode the space character in the same way as Javascript.
    // Javascript expects a space character to be encoded as "%20", whereas Java encodes it as "+".
    private fun String.urlEncodeForJavascript() = URIUtil.encodePath(this)
}
