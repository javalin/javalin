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
import java.nio.charset.Charset

object MultipartUtil {
    const val MULTIPART_CONFIG_ATTRIBUTE = "org.eclipse.jetty.multipartConfig"
    private val defaultConfig = MultipartConfigElement(System.getProperty("java.io.tmpdir"), -1, -1, 1)

    var preUploadFunction: (HttpServletRequest) -> Unit = { req ->
        if (req.getAttribute(MULTIPART_CONFIG_ATTRIBUTE) == null) {
            req.setAttribute(MULTIPART_CONFIG_ATTRIBUTE, defaultConfig)
        }
    }

    fun getUploadedFiles(req: HttpServletRequest, partName: String): List<UploadedFile> {
        preUploadFunction(req)
        return req.parts.filter { isFile(it) && it.name == partName }.map { UploadedFile(it) }
    }

    fun getUploadedFiles(req: HttpServletRequest): List<UploadedFile> {
        preUploadFunction(req)
        return req.parts.filter(this::isFile).map { UploadedFile(it) }
    }

    fun getUploadedFileMap(req: HttpServletRequest): Map<String, List<UploadedFile>> {
        preUploadFunction(req)
        return req
            .parts
            .filter(this::isFile)
            .groupBy { it.name }
            .mapValues { entry -> entry.value.map { UploadedFile(it) } }
    }

    fun getFieldMap(req: HttpServletRequest): Map<String, List<String>> {
        preUploadFunction(req)
        return req.parts.associate { part -> part.name to getPartValue(req, part.name) }
    }

    private fun getPartValue(req: HttpServletRequest, partName: String): List<String> {
        return req.parts.filter { isField(it) && it.name == partName }.map { part ->
            part.inputStream.use { it.readBytes().toString(Charset.forName("UTF-8")) }
        }.toList()
    }

    private fun isField(filePart: Part) = filePart.submittedFileName == null // this is what Apache FileUpload does ...
    private fun isFile(filePart: Part) = !isField(filePart)
}
