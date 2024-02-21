package io.javalin.community.ssl

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SslConfigExceptionTest {
    @Test
    fun `test all exception types`(){
        assertEquals("There is no certificate or key file provided",SslConfigException(SslConfigException.Types.MISSING_CERT_AND_KEY_FILE).message)
        assertEquals("Both the certificate and key must be provided using the same method",SslConfigException(SslConfigException.Types.MULTIPLE_IDENTITY_LOADING_OPTIONS).message)
    }
}
