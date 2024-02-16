/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.util

import io.javalin.http.Context
import io.javalin.http.UploadedFile
import io.javalin.http.servlet.JavalinServletContext
import jakarta.servlet.MultipartConfigElement
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.Part
import kotlin.text.Charsets.UTF_8

object MultipartUtil {

    const val MULTIPART_CONFIG_ATTRIBUTE = "org.eclipse.jetty.multipartConfig"

    private val defaultConfig = MultipartConfigElement(System.getProperty("java.io.tmpdir"), -1, -1, 1)

    var preUploadFunction: (HttpServletRequest) -> Unit = {
        if (it.getAttribute(MULTIPART_CONFIG_ATTRIBUTE) == null) {
            it.setAttribute(MULTIPART_CONFIG_ATTRIBUTE, defaultConfig)
        }
    }

    private inline fun <R> Context.processParts(body: (Sequence<Part>, Int) -> R): R {
        if ((this as JavalinServletContext).bodyInitialized) {
            throw IllegalStateException("Request body has already been consumed")
        }
        preUploadFunction(this.req())
        val parts = this.req().parts
        return body(parts.asSequence(), parts.size)
    }

    fun getUploadedFiles(ctx: Context, partName: String): List<UploadedFile> =
        ctx.processParts { parts, size ->
            parts
                .filter { isFile(it) && it.name == partName }
                .mapTo(ArrayList(size)) { UploadedFile(it) }
        }

    fun getUploadedFiles(ctx: Context): List<UploadedFile> =
        ctx.processParts { parts, size ->
            parts
                .filter(::isFile)
                .mapTo(ArrayList(size)) { UploadedFile(it) }
        }

    fun getUploadedFileMap(ctx: Context): Map<String, List<UploadedFile>> =
        ctx.processParts { parts, size ->
            parts
                .filter(::isFile)
                .groupByTo(HashMap(size), { it.name }, { UploadedFile(it) })
        }

    fun getFieldMap(ctx: Context): Map<String, List<String>> =
        ctx.processParts { parts, size ->
            parts.associateTo(HashMap(size)) { it.name to getPartValue(ctx, it.name) }
        }

    private fun getPartValue(ctx: Context, partName: String): List<String> =
        ctx.processParts { parts, size ->
            parts
                .filter { isField(it) && it.name == partName }
                .mapTo(ArrayList(size)) { part -> part.inputStream.use { it.readBytes().toString(UTF_8) } }
        }

    private fun isField(filePart: Part): Boolean =
        filePart.submittedFileName == null // this is what Apache FileUpload does

    private fun isFile(filePart: Part): Boolean =
        !isField(filePart)

}
