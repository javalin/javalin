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
 * [filename]: the file-name reported by the client, without any directory components
 * [extension]: the file-extension, extracted from the [filename]
 * @see Context.uploadedFile
 * @see <a href="https://javalin.io/documentation#faq">Uploads in FAQ</a>
 */
class UploadedFile(private val part: Part) {
    fun content(): InputStream = part.inputStream
    fun <T> contentAndClose(callback: (InputStream) -> T) = content().use { callback(it) }
    fun contentType(): String? = part.contentType
    /** The client-supplied file name reduced to its last path segment, so it can't point outside a target directory. */
    fun filename(): String = part.submittedFileName.substringAfterLast('/').substringAfterLast('\\').let { if (it == "." || it == "..") "" else it }
    fun extension(): String = filename().replaceBeforeLast(".", "")
    fun size(): Long = part.size
}
