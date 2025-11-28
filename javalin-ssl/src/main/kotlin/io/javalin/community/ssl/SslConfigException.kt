package io.javalin.community.ssl

import io.javalin.util.JavalinException

/**
 * Exception thrown when the SslConfig is invalid.
 */
class SslConfigException : JavalinException {
    constructor(type: Types) : super(type.message)

    /**
     * Types of errors that can occur when configuring SSL.
     */
    enum class Types(val message: String) {
        MISSING_CERT_AND_KEY_FILE("There is no certificate or key file provided"),
        MULTIPLE_IDENTITY_LOADING_OPTIONS("Both the certificate and key must be provided using the same method")
    }
}
