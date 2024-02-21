package io.javalin.community.ssl

import io.javalin.Javalin
import okhttp3.Request
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("integration")
class APITests : IntegrationTestClass(){

    @Test
    fun `plugin can be loaded using factory method`() {
        Javalin.create { config ->
            config.registerPlugin(SslPlugin{
                it.insecurePort = 9999
                it.secure = false
            })
        }
            .get("/") { ctx -> ctx.result("Hello World") }
            .start()

        untrustedClient.newCall(Request.Builder().url("http://localhost:9999/").build()).execute().use {
            assert(it.isSuccessful)
        }
    }


    @Suppress("UNREACHABLE_CODE","UNUSED_VARIABLE")
    fun `complete api compiles`(){
        //Dummy variables to make sure the code compiles
        val secondCertInputStream: java.io.InputStream = TODO()
        val keyInputStream: java.io.InputStream = TODO()
        val certString: String = TODO()
        val keyString: String = TODO()
        val keystoreInputStream: java.io.InputStream = TODO()
        val keyPassword: String = TODO()

        val plugin = SslPlugin {
               // Connection options
            it.host=null                                                            // Host to bind to, by default it will bind to all interfaces
            it.insecure=true                                                        // Toggle the default http (insecure) connector
            it.secure=true                                                          // Toggle the default https (secure) connector
            it.http2=true                                                           // Toggle HTTP/2 Support

            it.securePort=443                                                       // Port to use on the SSL (secure) connector (TCP)
            it.insecurePort=80                                                      // Port to use on the http (insecure) connector (TCP)
            it.redirect=false                                                       // Redirect all http requests to https

            it.sniHostCheck=true                                                    // Enable SNI hostname verification
            it.tlsConfig=TlsConfig.INTERMEDIATE                                     // Set the TLS configuration. (by default it uses Mozilla's intermediate configuration)

               // PEM loading options (mutually exclusive)
            it.pemFromPath("/path/to/cert.pem","/path/to/key.pem")                  // load from the given paths
            it.pemFromPath("/path/to/cert.pem","/path/to/key.pem","keyPassword")    // load from the given paths with the given key password
            it.pemFromClasspath("certName.pem","keyName.pem")                       // load from the given paths in the classpath
            it.pemFromClasspath("certName.pem","keyName.pem","keyPassword")         // load from the given paths in the classpath with the given key password
            it.pemFromInputStream(secondCertInputStream,keyInputStream)                   // load from the given input streams
            it.pemFromInputStream(secondCertInputStream,keyInputStream,"keyPassword")     // load from the given input streams with the given key password
            it.pemFromString(certString,keyString)                                  // load from the given strings
            it.pemFromString(certString,keyString,"keyPassword")                    // load from the given strings with the given key password

               // Keystore loading options (PKCS#12/JKS) (mutually exclusive)
            it.keystoreFromPath("/path/to/keystore.jks","keystorePassword")         // load the keystore from the given path
            it.keystoreFromClasspath("keyStoreName.p12","keystorePassword")         // load the keystore from the given path in the classpath
            it.keystoreFromInputStream(keystoreInputStream,"keystorePassword")      // load the keystore from the given input stream

               // Advanced options
            it.configConnectors { con -> con.dump() }                                // Set a Consumer to configure the connectors
            it.securityProvider = null                                              // Use a custom security provider
            it.withTrustConfig { trust -> trust.pemFromString("cert") }              // Set the trust configuration, explained below. (by default all clients are trusted)
        }

        SslPlugin{ config ->
            config.withTrustConfig{
                   // Certificate loading options (PEM/DER/P7B)
                it.certificateFromPath("path/to/certificate.pem")              // load a PEM/DER/P7B cert from the given path
                it.certificateFromClasspath("certificateName.pem")             // load a PEM/DER/P7B cert from the given path in the classpath
                it.certificateFromInputStream(secondCertInputStream)                 // load a PEM/DER/P7B cert from the given input stream
                it.p7bCertificateFromString("p7b encoded certificate")         // load a P7B cert from the given string
                it.pemFromString("pem encoded certificate")                    // load a PEM cert from the given string

                   // Trust store loading options (JKS/PKCS12)
                it.trustStoreFromPath("path/to/truststore.jks", "password")    // load a trust store from the given path
                it.trustStoreFromClasspath("truststore.jks", "password")       // load a trust store from the given path in the classpath
                it.trustStoreFromInputStream(secondCertInputStream, "password")      // load a trust store from the given input stream
            }
        }

        Javalin.create {
            it.registerPlugin(plugin)
        }

        plugin.reload{
            // any options other than loading certificates/keys will be ignored.
            it.pemFromPath("/path/to/new/cert.pem","/path/to/new/key.pem")

            // you can also replace trust configuration
            it.withTrustConfig{ trust ->
                trust.certificateFromPath("path/to/new/certificate.pem")
            }
        }
    }

}
