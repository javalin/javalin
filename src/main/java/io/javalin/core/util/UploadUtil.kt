/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.UploadedFile
import org.apache.commons.fileupload.servlet.ServletFileUpload
import java.io.ByteArrayInputStream
import javax.servlet.http.HttpServletRequest

object UploadUtil {

    fun getUploadedFiles(servletRequest: HttpServletRequest, partName: String): List<UploadedFile> {
        if (!ServletFileUpload.isMultipartContent(servletRequest)) {
            return listOf()
        }
        val iterator = ServletFileUpload().getItemIterator(servletRequest)
        val files = mutableListOf<UploadedFile>()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.fieldName == partName && !item.isFormField) {
                // if it's not a form field, then it's a file-field...
                // this is straight from the docs: https://commons.apache.org/proper/commons-fileupload/streaming.html
                files.add(
                        UploadedFile(
                                contentType = item.contentType,
                                content = ByteArrayInputStream(item.openStream().readBytes()),
                                name = item.name,
                                extension = item.name.replaceBeforeLast(".", "")
                        )
                )
            }
        }
        return files
    }

}

