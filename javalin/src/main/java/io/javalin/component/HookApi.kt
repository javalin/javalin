package io.javalin.component

import io.javalin.http.Context

data class Hook<COMPONENT : Any?>(
    override val id: String
) : IdentifiableHook

fun interface Resolver<COMPONENT : Any?> {
    fun resolve(ctx: Context?): COMPONENT
}
