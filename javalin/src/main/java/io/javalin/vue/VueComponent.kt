package io.javalin.vue

import io.javalin.http.Context

class VueComponent @JvmOverloads constructor(
    val component: String, val state: Any? = null,
    private val renderer: VueRenderer = VueRenderer()
) : VueHandler(component) {
    override fun state(ctx: Context) = this.state // we are extending VueHandler and just returning the state passed by the user
    override fun preRender(layout: String, ctx: Context) = renderer.preRender(layout, ctx) // default implementation does no pre rendering
    override fun postRender(layout: String, ctx: Context) = renderer.postRender(layout, ctx) // default implementation does no post rendering
}
