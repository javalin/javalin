package io.javalin.vue

import io.javalin.http.Context
import io.javalin.json.jsonMapper
import java.net.URLEncoder

internal object VueStateRenderer {
    fun getState(ctx: Context, state: Any?): String {
        val cfg = ctx.appAttribute<JavalinVueConfig>(JAVALINVUE_CONFIG_KEY)
        fun urlEncodedState(state: Any?) = ctx.jsonMapper().toJsonString(
            mapOf(
                "pathParams" to ctx.pathParamMap(),
                "state" to (state ?: cfg.stateFunction(ctx))
            )
        ).urlEncodeForJavascript()

        val prototypeOrGlobalConfig = if (cfg.vueAppName != null) "${cfg.vueAppName}.config.globalProperties" else "Vue.prototype"
        return """
            <script nonce="@internalAddNonce">
                $prototypeOrGlobalConfig.${'$'}javalin = JSON.parse(decodeURIComponent('${urlEncodedState(state)}'))
            </script>
        """.trimIndent()
    }

    // Unfortunately, Java's URLEncoder does not encode the space character in the same way as Javascript.
    // Javascript expects a space character to be encoded as "%20", whereas Java encodes it as "+".
    // All other encodings are implemented correctly, therefore we can simply replace the character in the encoded String.
    private fun String.urlEncodeForJavascript() = URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
}
