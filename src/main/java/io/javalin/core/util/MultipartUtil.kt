/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.UploadedFile
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import javax.servlet.MultipartConfigElement
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.Part

object MultipartUtil {

    fun getUploadedFiles(servletRequest: HttpServletRequest, partName: String): List<UploadedFile> {
        servletRequest.setAttribute("org.eclipse.jetty.multipartConfig", MultipartConfigElement(System.getProperty("java.io.tmpdir")))
        return servletRequest.parts.filter { isFile(it) && it.name == partName }.map { filePart ->
            UploadedFile(
                    contentType = filePart.contentType,
                    content = ByteArrayInputStream(filePart.inputStream.readBytes()),
                    name = filePart.submittedFileName,
                    extension = filePart.submittedFileName.replaceBeforeLast(".", "")
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
