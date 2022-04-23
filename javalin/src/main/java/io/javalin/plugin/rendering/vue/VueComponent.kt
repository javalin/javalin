package io.javalin.plugin.rendering.vue

import io.javalin.http.Context

class VueComponent  @JvmOverloads constructor(val component: String, val state: Any? = null, val renderer: VueRenderer = VueRenderer()) : VueHandler() {
    override fun component(ctx: Context): String {
       return component;
    }

    override fun state(ctx: Context): Any? {
        return state;
    }

    override fun String.preRender(ctx: Context): String {
        return renderer.preRender(this,ctx);
    }

    override fun String.postRender(ctx:Context):String {
        return renderer.postRender(this,ctx);
    }
}
