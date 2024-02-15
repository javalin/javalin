/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.util

import io.javalin.http.UploadedFile
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

    private inline fun <R> HttpServletRequest.processParts(body: (Sequence<Part>, Int) -> R): R {
        preUploadFunction(this)
        val parts = this.parts
        return body(parts.asSequence(), parts.size)
    }

    fun getUploadedFiles(req: HttpServletRequest, partName: String): List<UploadedFile> =
        req.processParts { parts, size ->
            parts
                .filter { isFile(it) && it.name == partName }
                .mapTo(ArrayList(size)) { UploadedFile(it) }
        }

    fun getUploadedFiles(req: HttpServletRequest): List<UploadedFile> =
        req.processParts { parts, size ->
            parts
                .filter(::isFile)
                .mapTo(ArrayList(size)) { UploadedFile(it) }
        }

    fun getUploadedFileMap(req: HttpServletRequest): Map<String, List<UploadedFile>> =
        req.processParts { parts, size ->
            parts
                .filter(::isFile)
                .groupByTo(HashMap(size), { it.name }, { UploadedFile(it) })
        }

    fun getFieldMap(req: HttpServletRequest): Map<String, List<String>> =
        req.processParts { parts, size ->
            parts.associateTo(HashMap(size)) { it.name to getPartValue(req, it.name) }
        }

    private fun getPartValue(req: HttpServletRequest, partName: String): List<String> =
        req.processParts { parts, size ->
            parts
                .filter { isField(it) && it.name == partName }
                .mapTo(ArrayList(size)) { part -> part.inputStream.use { it.readBytes().toString(UTF_8) } }
        }

    private fun isField(filePart: Part): Boolean =
        filePart.submittedFileName == null // this is what Apache FileUpload does

    private fun isFile(filePart: Part): Boolean =
        !isField(filePart)

}
