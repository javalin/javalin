/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import jakarta.servlet.http.Part
import java.io.InputStream

/**
 * Data class containing the content and meta-info of an uploaded file.
 * [content]: the file-content as an [InputStream]
 * [contentType]: the content-type passed by the client
 * [size]: the size of the file in bytes
 * [filename]: the file-name reported by the client
 * [extension]: the file-extension, extracted from the [filename]
 * @see Context.uploadedFile
 * @see <a href="https://javalin.io/documentation#faq">Uploads in FAQ</a>
 */
class UploadedFile(private val part: Part) {
    fun content(): InputStream = part.inputStream
    fun <T> contentAndClose(callback: (InputStream) -> T) = content().use { callback(it) }
    fun contentType(): String? = part.contentType
    fun filename(): String = part.submittedFileName
    fun extension(): String = part.submittedFileName.replaceBeforeLast(".", "")
    fun size(): Long = part.size
}
