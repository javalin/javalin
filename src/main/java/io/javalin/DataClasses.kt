/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import java.io.InputStream

data class UploadedFile(val contentType: String, val content: InputStream, val name: String, val extension: String)

enum class LogLevel { EXTENSIVE, STANDARD, MINIMAL, OFF; }

data class BasicAuthCredentials(val username: String, val password: String)
