package io.javalin.security

/**
 * Auth credentials for basic HTTP authorization.
 * Contains the Base64 decoded [username] and [password] from the Authorization header.
 * @see io.javalin.http.Context.basicAuthCredentials
 */
data class BasicAuthCredentials(val username: String, val password: String)
