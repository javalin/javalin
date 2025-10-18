/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.util

import io.javalin.config.MultipartConfig
import io.javalin.http.UploadedFile
import io.javalin.http.servlet.JavalinServletRequest
import io.javalin.util.BodyAlreadyReadException
import jakarta.servlet.MultipartConfigElement
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.Part
import kotlin.text.Charsets.UTF_8

object MultipartUtil {

    const val MULTIPART_CONFIG_ATTRIBUTE = "org.eclipse.jetty.multipartConfig"

    private val defaultConfig = MultipartConfigElement(System.getProperty("java.io.tmpdir"), -1, -1, 1)

    private inline fun <R> HttpServletRequest.processParts(multipartConfig: MultipartConfig, body: (Sequence<Part>, Int) -> R): R {
        if ((this as JavalinServletRequest).inputStreamRead) {
            throw BodyAlreadyReadException("Request body has already been consumed. You cannot get multipart parts after reading the request body.")
        }
        // Apply multipart configuration if not already set
        if (getAttribute(MULTIPART_CONFIG_ATTRIBUTE) == null) {
            setAttribute(MULTIPART_CONFIG_ATTRIBUTE, multipartConfig.multipartConfigElement())
        }
        val parts = this.parts
        return body(parts.asSequence(), parts.size)
    }

    fun uploadedFiles(req: HttpServletRequest, partName: String, multipartConfig: MultipartConfig): List<UploadedFile> =
        req.processParts(multipartConfig) { parts, size ->
            parts
                .filter { isFile(it) && it.name == partName }
                .mapTo(ArrayList(size)) { UploadedFile(it) }
        }

    fun uploadedFiles(req: HttpServletRequest, multipartConfig: MultipartConfig): List<UploadedFile> =
        req.processParts(multipartConfig) { parts, size ->
            parts
                .filter(::isFile)
                .mapTo(ArrayList(size)) { UploadedFile(it) }
        }

    fun uploadedFileMap(req: HttpServletRequest, multipartConfig: MultipartConfig): Map<String, List<UploadedFile>> =
        req.processParts(multipartConfig) { parts, size ->
            parts
                .filter(::isFile)
                .groupByTo(HashMap(size), { it.name }, { UploadedFile(it) })
        }

    fun fieldMap(req: HttpServletRequest, multipartConfig: MultipartConfig): Map<String, List<String>> =
        req.processParts(multipartConfig) { parts, size ->
            parts.associateTo(HashMap(size)) { it.name to getPartValue(req, it.name, multipartConfig) }
        }

    private fun getPartValue(req: HttpServletRequest, partName: String, multipartConfig: MultipartConfig): List<String> =
        req.processParts(multipartConfig) { parts, size ->
            parts
                .filter { isField(it) && it.name == partName }
                .mapTo(ArrayList(size)) { part -> part.inputStream.use { it.readBytes().toString(UTF_8) } }
        }

    private fun isField(filePart: Part): Boolean =
        filePart.submittedFileName == null // this is what Apache FileUpload does

    private fun isFile(filePart: Part): Boolean =
        !isField(filePart)

}
