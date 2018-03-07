/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import java.io.InputStream

/**
 * Description of a file uploaded as a multipart content
 * @see Context.uploadedFile
 * @see <a href="https://javalin.io/documentation#faq">Uploads in FAQ</a>
 */
data class UploadedFile(val contentType: String, val content: InputStream, val name: String, val extension: String)

/**
 * Internal request logging level. Default is [LogLevel.OFF]
 * @see Javalin.enableStandardRequestLogging
 * @see Javalin.requestLogLevel
 */
enum class LogLevel { EXTENSIVE, STANDARD, MINIMAL, OFF; }

/**
 * Auth credentials for basic HTTP authorization.
 * @see Context.basicAuthCredentials
 * @see <a href="https://javalin.io/documentation#faq">Authorization in FAQ</a>
 */
data class BasicAuthCredentials(val username: String, val password: String)
