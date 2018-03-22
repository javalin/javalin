/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.UploadedFile
import java.io.ByteArrayInputStream
import javax.servlet.MultipartConfigElement
import javax.servlet.http.HttpServletRequest

object UploadUtil {
    fun getUploadedFiles(servletRequest: HttpServletRequest, partName: String): List<UploadedFile> {
        servletRequest.setAttribute("org.eclipse.jetty.multipartConfig", MultipartConfigElement(System.getProperty("java.io.tmpdir")));
        return servletRequest.parts.filter { it.name == partName }.map { filePart ->
            UploadedFile(
                    contentType = filePart.contentType,
                    content = ByteArrayInputStream(filePart.inputStream.readBytes()),
                    name = filePart.submittedFileName,
                    extension = filePart.submittedFileName.replaceBeforeLast(".", "")
            )
        }
    }
}
