package io.javalin.plugin.rendering.vue

import io.javalin.http.Context

class VueRenderer {

    open fun preRender(template: String, ctx: Context): String{
        return template;
    }

    open fun postRender(template: String, ctx:Context) : String{
        return template;
    }

}
