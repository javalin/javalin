package io.javalin.component

import io.javalin.http.Context

class ComponentHandle<T>

fun interface ComponentResolver<T> {
    fun resolve(ctx: Context?): T
}

class ComponentNotFoundException(
    handle: ComponentHandle<*>
) : IllegalStateException("No component resolver registered for ${handle}")

class ComponentAlreadyExistsException(
    handle: ComponentHandle<*>
) : IllegalStateException("Component resolver already registered for ${handle}")
