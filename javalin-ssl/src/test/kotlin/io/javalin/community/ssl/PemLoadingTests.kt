package io.javalin.community.ssl

import io.javalin.community.ssl.certs.Server
import nl.altindag.ssl.exception.GenericIOException
import nl.altindag.ssl.pem.exception.CertificateParseException
import nl.altindag.ssl.pem.exception.PrivateKeyParseException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.InputStream

@Tag("integration")
class PemLoadingTests : IntegrationTestClass() {
    @Test
    fun `Loading a passwordless PEM file from a string works`() {
        assertSslWorks { config: SslConfig ->
            config.pemFromString(
                Server.CERTIFICATE_AS_STRING,
                Server.NON_ENCRYPTED_KEY_AS_STRING
            )
        }
    }

    @Test
    fun `Loading a an invalid key PEM file from a string fails`() {
        Assertions.assertThrows(PrivateKeyParseException::class.java) {
            assertSslWorks { config: SslConfig ->
                config.pemFromString(
                    Server.CERTIFICATE_AS_STRING, "invalid"
                )
            }
        }
    }

    @Test
    fun `Loading a an invalid certificate PEM file from a string fails`() {
        Assertions.assertThrows(CertificateParseException::class.java) {
            assertSslWorks { config: SslConfig ->
                config.pemFromString(
                    "invalid",
                    Server.NON_ENCRYPTED_KEY_AS_STRING
                )
            }
        }
    }

    @Test
    fun `Loading a PEM file with a wrong password from a string fails`() {
        Assertions.assertThrows(PrivateKeyParseException::class.java) {
            assertSslWorks { config: SslConfig ->
                config.pemFromString(
                    Server.CERTIFICATE_AS_STRING, Server.ENCRYPTED_KEY_AS_STRING, "invalid"
                )
            }
        }
    }

    @Test
    fun `Loading an encrypted PEM file from a string works`() {
        assertSslWorks { config: SslConfig ->
            config.pemFromString(
                Server.CERTIFICATE_AS_STRING,
                Server.ENCRYPTED_KEY_AS_STRING,
                Server.KEY_PASSWORD
            )
        }
    }

    @Test
    fun `Loading a passwordless PEM file from the classpath works`() {
        assertSslWorks { config: SslConfig ->
            config.pemFromClasspath(
                Server.CERTIFICATE_FILE_NAME,
                Server.NON_ENCRYPTED_KEY_FILE_NAME
            )
        }
    }

    @Test
    fun `Loading an encrypted PEM file from the classpath works`() {
        assertSslWorks { config: SslConfig ->
            config.pemFromClasspath(
                Server.CERTIFICATE_FILE_NAME,
                Server.ENCRYPTED_KEY_FILE_NAME,
                Server.KEY_PASSWORD
            )
        }
    }

    @Test
    fun `Loading a PEM file with a wrong password from the classpath fails`() {
        Assertions.assertThrows(PrivateKeyParseException::class.java) {
            assertSslWorks { config: SslConfig ->
                config.pemFromClasspath(
                    Server.CERTIFICATE_FILE_NAME, Server.ENCRYPTED_KEY_FILE_NAME, "invalid"
                )
            }
        }
    }

    @Test
    fun `Loading a PEM file from an invalid classpath cert location fails`() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            assertSslWorks { config: SslConfig ->
                config.pemFromClasspath(
                    "invalid",
                    Server.NON_ENCRYPTED_KEY_FILE_NAME
                )
            }
        }
    }

    @Test
    fun `Loading a PEM file from an invalid classpath key location fails`() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            assertSslWorks { config: SslConfig ->
                config.pemFromClasspath(
                    Server.CERTIFICATE_FILE_NAME, "invalid"
                )
            }
        }
    }

    @Test
    fun `Loading a passwordless PEM file from a path works`() {
        assertSslWorks { config: SslConfig ->
            config.pemFromPath(
                Server.CERTIFICATE_PATH,
                Server.NON_ENCRYPTED_KEY_PATH
            )
        }
    }

    @Test
    fun `Loading an encrypted PEM file from a path works`() {
        assertSslWorks { config: SslConfig ->
            config.pemFromPath(
                Server.CERTIFICATE_PATH,
                Server.ENCRYPTED_KEY_PATH,
                Server.KEY_PASSWORD
            )
        }
    }

    @Test
    fun `Loading a PEM file with a wrong password from a path fails`() {
        Assertions.assertThrows(PrivateKeyParseException::class.java) {
            assertSslWorks { config: SslConfig ->
                config.pemFromPath(
                    Server.CERTIFICATE_PATH, Server.ENCRYPTED_KEY_PATH, "invalid"
                )
            }
        }
    }

    @Test
    fun `Loading a PEM file from an invalid cert path fails`() {
        Assertions.assertThrows(GenericIOException::class.java) {
            assertSslWorks { config: SslConfig ->
                config.pemFromPath(
                    "invalid",
                    Server.NON_ENCRYPTED_KEY_PATH
                )
            }
        }
    }

    @Test
    fun `Loading a PEM file from an invalid key path fails`() {
        Assertions.assertThrows(GenericIOException::class.java) {
            assertSslWorks { config: SslConfig ->
                config.pemFromPath(
                    Server.CERTIFICATE_PATH, "invalid"
                )
            }
        }
    }

    @Test
    fun `Loading a passwordless PEM file from an input stream works`() {
        assertSslWorks { config: SslConfig ->
            config.pemFromInputStream(
                Server.CERTIFICATE_INPUT_STREAM_SUPPLIER.get(),
                Server.NON_ENCRYPTED_KEY_INPUT_STREAM_SUPPLIER.get()
            )
        }
    }

    @Test
    fun `Loading an encrypted PEM file from an input stream works`() {
        assertSslWorks { config: SslConfig ->
            config.pemFromInputStream(
                Server.CERTIFICATE_INPUT_STREAM_SUPPLIER.get(),
                Server.ENCRYPTED_KEY_INPUT_STREAM_SUPPLIER.get(),
                Server.KEY_PASSWORD
            )
        }
    }

    @Test
    fun `Loading a PEM file with a wrong password from an input stream fails`() {
        Assertions.assertThrows(PrivateKeyParseException::class.java) {
            assertSslWorks { config: SslConfig ->
                config.pemFromInputStream(
                    Server.CERTIFICATE_INPUT_STREAM_SUPPLIER.get(),
                    Server.ENCRYPTED_KEY_INPUT_STREAM_SUPPLIER.get(),
                    "invalid"
                )
            }
        }
    }

    @Test
    fun `Loading a PEM file from an invalid cert input stream fails`() {
        Assertions.assertThrows(CertificateParseException::class.java) {
            assertSslWorks { config: SslConfig ->
                config.pemFromInputStream(
                    InputStream.nullInputStream(), Server.NON_ENCRYPTED_KEY_INPUT_STREAM_SUPPLIER.get()
                )
            }
        }
    }

    @Test
    fun `Loading a PEM file from an invalid key input stream fails`() {
        Assertions.assertThrows(PrivateKeyParseException::class.java) {
            assertSslWorks { config: SslConfig ->
                config.pemFromInputStream(
                    Server.ENCRYPTED_KEY_INPUT_STREAM_SUPPLIER.get(), InputStream.nullInputStream()
                )
            }
        }
    }
}
