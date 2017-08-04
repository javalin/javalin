/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import org.apache.commons.io.FileUtils
import java.io.File
import java.io.InputStream

data class UploadedFile(val contentType: String, val content: InputStream, val name: String, val extension: String){
    fun getFile(): File {
        val uploadedFile = createTempFile()
        FileUtils.copyInputStreamToFile(this.content, uploadedFile)
        return uploadedFile
    }
}

