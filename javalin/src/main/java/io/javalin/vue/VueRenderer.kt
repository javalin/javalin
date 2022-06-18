package io.javalin.vue

import io.javalin.http.Context

open class VueRenderer {
    open fun preRender(layout: String, ctx: Context) = layout // no changes by default
    open fun postRender(layout: String, ctx: Context) = layout // no changes by default
}
