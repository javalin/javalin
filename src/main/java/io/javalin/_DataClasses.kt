/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.HandlerType
import io.javalin.security.Role
import java.io.InputStream

/**
 * Data class containing the content and meta-info of an uploaded file.
 * [contentType]: the content-type passed by the client
 * [inputStream]: the file-content as an [InputStream]
 * [name]: the file-name reported by the client
 * [extension]: the file-extension, extracted from the [name]
 * @see Context.uploadedFile
 * @see <a href="https://javalin.io/documentation#faq">Uploads in FAQ</a>
 */
data class UploadedFile(val contentType: String, val content: InputStream, val name: String, val extension: String)

/**
 * Auth credentials for basic HTTP authorization.
 * Contains the Base64 decoded [username] and [password] from the Authorization header.
 * @see Context.basicAuthCredentials
 */
data class BasicAuthCredentials(val username: String, val password: String)

/**
 * Server lifecycle events
 * @see Javalin.event
 */
enum class JavalinEvent {
    SERVER_STARTING,
    SERVER_STARTED,
    SERVER_START_FAILED,
    SERVER_STOPPING,
    SERVER_STOPPED
}

data class HandlerMetaInfo(val httpMethod: HandlerType, val path: String, val handler: Any, val roles: Set<Role>)
