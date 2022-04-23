package io.javalin.plugin.rendering.vue

import io.javalin.http.Context

class VueComponent  @JvmOverloads constructor(val component: String, val state: Any? = null, val renderer: VueRenderer? = null) : VueHandler() {
    override fun component(ctx: Context): String {
       return this.component;
    }

    override fun state(ctx: Context): Any? {
        return this.state;
    }

    override fun preRender(template: String, ctx: Context): String {
        return renderer?.preRender(template, ctx) ?: template;
    }

    override fun postRender(template: String, ctx:Context):String {
        return renderer?.postRender(template, ctx) ?: template;
    }
}
