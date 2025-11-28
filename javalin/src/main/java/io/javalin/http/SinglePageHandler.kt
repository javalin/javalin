/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.http.servlet.isLocalhost
import io.javalin.http.staticfiles.Location
import io.javalin.util.Util
import java.net.URL

/**
 * This is just a glorified 404 handler.
 * Ex: app.singlePage.addRootFile("/my-path", "index.html")
 * If no routes or static files are found on "/my-path/" (or any subpath), index.html will be returned
 *
 * It also supports custom handlers (as opposed to a file path like above).
 * Ex: app.singlePage.addRootHandler("/my-path", myHandler)
 * If no routes or static files or single page file paths are found on "/my-path/" (or any subpath), myHandler will handle the request.
 */
class SinglePageHandler {

    data class Page(val url: URL, val cachedHtml: String) {
        fun getHtml(reRead: Boolean) = if (reRead) url.readText() else cachedHtml
    }

    private val pathPageMap = mutableMapOf<String, Page>()
    private val pathHandlerMap = mutableMapOf<String, Handler>()

    fun add(hostedPath: String, filePath: String, location: Location) {
        val url = when (location) {
            Location.CLASSPATH -> Util.resourceUrl(filePath.removePrefix("/")) ?: throw IllegalArgumentException("File at '$filePath' not found. Path should be relative to resource folder.")
            Location.EXTERNAL -> Util.fileUrl(filePath) ?: throw IllegalArgumentException("External file at '$filePath' not found.")
        }
        pathPageMap[hostedPath] = Page(url, url.readText())
    }

    fun add(hostedPath: String, handler: Handler) {
        pathHandlerMap[hostedPath] = handler
    }

    fun canHandle(ctx: Context): Boolean {
        val accept = ctx.header(Header.ACCEPT) ?: ""
        return when {
            ContentType.HTML !in accept && "*/*" !in accept && accept != "" -> false
            pathPageMap.findByPath(ctx.path()) != null -> true
            pathHandlerMap.findByPath(ctx.path()) != null -> true
            else -> false
        }
    }

    fun handle(ctx: Context): Boolean {
        val accept = ctx.header(Header.ACCEPT) ?: ""
        if (ContentType.HTML !in accept && "*/*" !in accept && accept != "") return false
        pathPageMap.findByPath(ctx.path())?.let { page ->
            ctx.html(page.getHtml(reRead = ctx.isLocalhost()))
            return true
        }
        pathHandlerMap.findByPath(ctx.path())?.let { handler ->
            handler.handle(ctx)
            return true
        }
        return false
    }

}

private fun <T> Map<String, T>.findByPath(requestPath: String) = this.keys.find { requestPath.startsWith(it) }?.let { this[it]!! }
