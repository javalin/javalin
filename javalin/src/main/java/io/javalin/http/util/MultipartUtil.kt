/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.util

import io.javalin.http.UploadedFile
import java.nio.charset.Charset
import javax.servlet.MultipartConfigElement
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.Part

object MultipartUtil {

    var preUploadFunction: (HttpServletRequest) -> Unit = { req ->
        req.setAttribute("org.eclipse.jetty.multipartConfig", MultipartConfigElement(System.getProperty("java.io.tmpdir")))
    }

    fun getUploadedFiles(req: HttpServletRequest, partName: String): List<UploadedFile> {
        preUploadFunction(req)
        return req.parts.filter { isFile(it) && it.name == partName }.map(this::toUploadedFile)
    }

    fun getUploadedFiles(req: HttpServletRequest): List<UploadedFile> {
        preUploadFunction(req)
        return req.parts.filter(this::isFile).map(this::toUploadedFile)
    }

    fun getFieldMap(req: HttpServletRequest): Map<String, List<String>> {
        preUploadFunction(req)
        return req.parts.associate { part -> part.name to getPartValue(req, part.name) }
    }

    private fun getPartValue(req: HttpServletRequest, partName: String): List<String> {
        return req.parts.filter { isField(it) && it.name == partName }.map { filePart ->
            filePart.inputStream.readBytes().toString(Charset.forName("UTF-8"))
        }.toList()
    }

    private fun toUploadedFile(filePart: Part): UploadedFile {
        return UploadedFile(
                content = filePart.inputStream,
                contentType = filePart.contentType,
                filename = filePart.submittedFileName,
                extension = filePart.submittedFileName.replaceBeforeLast(".", ""),
                size = filePart.size
        )
    }

    private fun isField(filePart: Part) = filePart.submittedFileName == null // this is what Apache FileUpload does ...
    private fun isFile(filePart: Part) = !isField(filePart)
}
