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

    fun getUploadedFiles(servletRequest: HttpServletRequest, partName: String): List<UploadedFile> {
        preUploadFunction(servletRequest)
        return servletRequest.parts.filter { isFile(it) && it.name == partName }.map { filePart ->
            UploadedFile(
                    content = filePart.inputStream,
                    contentType = filePart.contentType,
                    contentLength = filePart.size.toInt(),
                    filename = filePart.submittedFileName,
                    extension = filePart.submittedFileName.replaceBeforeLast(".", ""),
                    size = filePart.size
            )
        }
    }

    fun getFieldMap(req: HttpServletRequest): Map<String, List<String>> {
        req.setAttribute("org.eclipse.jetty.multipartConfig", MultipartConfigElement(System.getProperty("java.io.tmpdir")))
        return req.parts.associate { part -> part.name to getPartValue(req, part.name) }
    }

    private fun getPartValue(req: HttpServletRequest, partName: String): List<String> {
        return req.parts.filter { isField(it) && it.name == partName }.map { filePart ->
            filePart.inputStream.readBytes().toString(Charset.forName("UTF-8"))
        }.toList()
    }

    private fun isField(filePart: Part) = filePart.submittedFileName == null // this is what Apache FileUpload does ...
    private fun isFile(filePart: Part) = !isField(filePart)
}
