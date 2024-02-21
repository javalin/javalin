package io.javalin.community.ssl;

import io.javalin.Javalin;

import java.io.InputStream;

public class JavaAPITests {

    static void javaApiCompiles(){
        InputStream certInputStream = null;
        InputStream keyInputStream = null;
        String certString = null;
        String keyString = null;
        InputStream keystoreInputStream = null;
        String keyPassword = null;

        SslPlugin plugin = new SslPlugin(conf ->{
            // Connection options
            conf.host = null;                                                           // Host to bind to, by default it will bind to all interfaces
            conf.insecure = true;                                                       // Toggle the default http (insecure) connector
            conf.secure = true;                                                         // Toggle the default https (secure) connector
            conf.http2 = true;                                                          // Toggle HTTP/2 Support

            conf.securePort = 443;                                                      // Port to use on the SSL (secure) connector (TCP)
            conf.insecurePort = 80;                                                     // Port to use on the http (insecure) connector (TCP)
            conf.redirect = false;                                                      // Redirect all http requests to https

            conf.sniHostCheck = true;                                                   // Enable SNI hostname verification
            conf.tlsConfig = TlsConfig.INTERMEDIATE;                                    // Set the TLS configuration. (by default it uses Mozilla's intermediate configuration)

            // PEM loading options (mutually exclusive)
            conf.pemFromPath("/path/to/cert.pem", "/path/to/key.pem");                  // load from the given paths
            conf.pemFromPath("/path/to/cert.pem", "/path/to/key.pem", "keyPassword");   // load from the given paths with the given key password
            conf.pemFromClasspath("certName.pem", "keyName.pem");                       // load from the given paths in the classpath
            conf.pemFromClasspath("certName.pem", "keyName.pem", "keyPassword");        // load from the given paths in the classpath with the given key password
            conf.pemFromInputStream(certInputStream, keyInputStream);                   // load from the given input streams
            conf.pemFromInputStream(certInputStream, keyInputStream, "keyPassword");    // load from the given input streams with the given key password
            conf.pemFromString(certString, keyString);                                  // load from the given strings
            conf.pemFromString(certString, keyString, "keyPassword");                   // load from the given strings with the given key password

            // Keystore loading options (PKCS#12/JKS) (mutually exclusive)
            conf.keystoreFromPath("/path/to/keystore.jks", "keystorePassword");         // load the keystore from the given path
            conf.keystoreFromClasspath("keyStoreName.p12", "keystorePassword");         // load the keystore from the given path in the classpath
            conf.keystoreFromInputStream(keystoreInputStream, "keystorePassword");      // load the keystore

            // Advanced options
            conf.configConnectors(con -> con.dump());                                   // Set a Consumer to configure the connectors
            conf.securityProvider = null;                                               // Use a custom security provider
            conf.withTrustConfig(trust -> trust.pemFromString("cert"));                 // Set the trust configuration, explained below. (by default all clients are trusted)
        });

        SslPlugin trustPlugin = new SslPlugin(conf ->{
            conf.withTrustConfig(trust ->{
                // Certificate loading options (PEM/DER/P7B)
                trust.certificateFromPath("path/to/certificate.pem");              // load a PEM/DER/P7B cert from the given path
                trust.certificateFromClasspath("certificateName.pem");             // load a PEM/DER/P7B cert from the given path in the classpath
                trust.certificateFromInputStream(certInputStream);                 // load a PEM/DER/P7B cert from the given input stream
                trust.p7bCertificateFromString("p7b encoded certificate");         // load a P7B cert from the given string
                trust.pemFromString("pem encoded certificate");                    // load a PEM cert from the given string

                // Trust store loading options (JKS/PKCS12)
                trust.trustStoreFromPath("path/to/truststore.jks", "password");    // load a trust store from the given path
                trust.trustStoreFromClasspath("truststore.jks", "password");       // load a trust store from the given path in the classpath
                trust.trustStoreFromInputStream(certInputStream, "password");      // load a trust store from the given input stream
            });
        });

        Javalin.create(conf -> conf.registerPlugin(plugin));

        plugin.reload(conf ->{
            // any options other than loading certificates/keys will be ignored.
            conf.pemFromPath("/path/to/new/cert.pem", "/path/to/new/key.pem");

            // you can also replace trust configuration
            conf.withTrustConfig(trust ->{
                trust.certificateFromPath("path/to/new/certificate.pem");
            });
        });
    }
}
