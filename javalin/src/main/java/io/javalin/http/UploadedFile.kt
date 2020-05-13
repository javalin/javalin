/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

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
data class UploadedFile(val content: InputStream, val contentType: String, @Deprecated("Use UploadedFile.size", replaceWith = ReplaceWith("size")) val contentLength: Int, val filename: String, val extension: String, val size: Long)
