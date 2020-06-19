/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.core.util.Header
import io.javalin.core.util.Util
import io.javalin.http.staticfiles.Location
import io.javalin.http.util.ContextUtil.isLocalhost
import java.net.URL

/**
 * This is just a glorified 404 handler.
 * Ex: app.addSinglePageRoot("/my-path", "index.html")
 * If no routes or static files are found on "/my-path/" (or any subpath), index.html will be returned
 *
 * It also supports custom handlers (as opposed to a file path like above).
 * Ex: app.addSinglePageHandler("/my-path", myHandler)
 * If no routes or static files or single page file paths are found on "/my-path/" (or any subpath), myHandler will handle the request.
 */
class SinglePageHandler {

    private val pathUrlMap = mutableMapOf<String, URL>()
    private val pathPageMap = mutableMapOf<String, String>()
    private val pathCustomHandlerMap = mutableMapOf<String, Handler>()

    fun add(path: String, filePath: String, location: Location) {
        pathUrlMap[path] = when (location) {
            Location.CLASSPATH -> Util.getResourceUrl(filePath.removePrefix("/")) ?: throw IllegalArgumentException("File at '$filePath' not found. Path should be relative to resource folder.")
            Location.EXTERNAL -> Util.getFileUrl(filePath) ?: throw IllegalArgumentException("External file at '$filePath' not found.")
        }
        pathPageMap[path] = pathUrlMap[path]!!.readText()
    }

    fun add(path: String, customHandler: Handler) {
        pathCustomHandlerMap[path] = customHandler
    }

    fun handle(ctx: Context): Boolean {
        val accepts = ctx.header(Header.ACCEPT) ?: ""
        if (accepts.contains("text/html") || accepts.contains("*/*") || accepts == "") {
            for (path in pathPageMap.keys) {
                if (ctx.path().startsWith(path)) {
                    ctx.html(when (ctx.isLocalhost()) {
                        true -> pathUrlMap[path]!!.readText() // is localhost, read file again
                        false -> pathPageMap[path]!! // not localhost, use cached content
                    })
                    return true
                }
            }
            for ((path, customHandler) in pathCustomHandlerMap) {
                if (ctx.path().startsWith(path)) {
                    customHandler.handle(ctx)
                    return true
                }
            }
        }
        return false
    }

}
