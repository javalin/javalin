/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.security.Role
import java.util.*

object ApiBuilder {

    private var staticJavalin: Javalin? = null
    private val pathDeque = ArrayDeque<String>()

    @FunctionalInterface
    interface EndpointGroup {
        fun addEndpoints()
    }

    @JvmStatic fun setStaticJavalin(javalin: Javalin) {
        staticJavalin = javalin
    }

    @JvmStatic fun clearStaticJavalin() {
        staticJavalin = null
    }

    @JvmStatic fun path(path: String, endpointGroup: EndpointGroup) {
        pathDeque.addLast(path)
        endpointGroup.addEndpoints()
        pathDeque.removeLast()
    }

    private fun prefixPath(path: String): String {
        return pathDeque.joinToString("") + path
    }

    private fun staticInstance(): Javalin {
        if (staticJavalin == null) {
            throw IllegalStateException("The static API can only be called within a routes() call")
        }
        return staticJavalin!!;
    }

    // Everything below here is copied from the end of Javalin.java

    //
    // HTTP verbs
    //
    @JvmStatic fun get(path: String, handler: Handler) {
        staticInstance().get(prefixPath(path), handler)
    }

    @JvmStatic fun post(path: String, handler: Handler) {
        staticInstance().post(prefixPath(path), handler)
    }

    @JvmStatic fun put(path: String, handler: Handler) {
        staticInstance().put(prefixPath(path), handler)
    }

    @JvmStatic fun patch(path: String, handler: Handler) {
        staticInstance().patch(prefixPath(path), handler)
    }

    @JvmStatic fun delete(path: String, handler: Handler) {
        staticInstance().delete(prefixPath(path), handler)
    }

    @JvmStatic fun head(path: String, handler: Handler) {
        staticInstance().head(prefixPath(path), handler)
    }

    @JvmStatic fun trace(path: String, handler: Handler) {
        staticInstance().trace(prefixPath(path), handler)
    }

    @JvmStatic fun connect(path: String, handler: Handler) {
        staticInstance().connect(prefixPath(path), handler)
    }

    @JvmStatic fun options(path: String, handler: Handler) {
        staticInstance().options(prefixPath(path), handler)
    }

    // Secured HTTP verbs
    @JvmStatic fun get(path: String, handler: Handler, permittedRoles: List<Role>) {
        staticInstance().get(prefixPath(path), handler, permittedRoles)
    }

    @JvmStatic fun post(path: String, handler: Handler, permittedRoles: List<Role>) {
        staticInstance().post(prefixPath(path), handler, permittedRoles)
    }

    @JvmStatic fun put(path: String, handler: Handler, permittedRoles: List<Role>) {
        staticInstance().put(prefixPath(path), handler, permittedRoles)
    }

    @JvmStatic fun patch(path: String, handler: Handler, permittedRoles: List<Role>) {
        staticInstance().patch(prefixPath(path), handler, permittedRoles)
    }

    @JvmStatic fun delete(path: String, handler: Handler, permittedRoles: List<Role>) {
        staticInstance().delete(prefixPath(path), handler, permittedRoles)
    }

    @JvmStatic fun head(path: String, handler: Handler, permittedRoles: List<Role>) {
        staticInstance().head(prefixPath(path), handler, permittedRoles)
    }

    @JvmStatic fun trace(path: String, handler: Handler, permittedRoles: List<Role>) {
        staticInstance().trace(prefixPath(path), handler, permittedRoles)
    }

    @JvmStatic fun connect(path: String, handler: Handler, permittedRoles: List<Role>) {
        staticInstance().connect(prefixPath(path), handler, permittedRoles)
    }

    @JvmStatic fun options(path: String, handler: Handler, permittedRoles: List<Role>) {
        staticInstance().options(prefixPath(path), handler, permittedRoles)
    }

    // Filters
    @JvmStatic fun before(path: String, handler: Handler) {
        staticInstance().before(prefixPath(path), handler)
    }

    @JvmStatic fun before(handler: Handler) {
        staticInstance().before(prefixPath("/*"), handler)
    }

    @JvmStatic fun after(path: String, handler: Handler) {
        staticInstance().after(prefixPath(path), handler)
    }

    @JvmStatic fun after(handler: Handler) {
        staticInstance().after(prefixPath("/*"), handler)
    }

}
