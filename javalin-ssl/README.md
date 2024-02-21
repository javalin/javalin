# About Javalin [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![Chat at https://discord.gg/sgak4e5NKv](https://img.shields.io/badge/chat-on%20Discord-%234cb697)](https://discord.gg/sgak4e5NKv)

* [:heart: Sponsor Javalin](https://github.com/sponsors/tipsy)
* The main project webpage is [javalin.io](https://javalin.io)
* Chat on Discord: <https://discord.gg/sgak4e5NKv>
* License summary: <https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)>

# SSL Plugin [![Coverage](https://codecov.io/gh/javalin/javalin-ssl/branch/dev/graphs/badge.svg)](https://app.codecov.io/gh/javalin/javalin-ssl) [![javadoc](https://javadoc.io/badge2/io.javalin.community.ssl/ssl-plugin/javadoc.svg)](https://javadoc.io/doc/io.javalin.community.ssl/ssl-plugin)

Straightforward SSL and HTTP/2 Configuration for Javalin!

If you're not familiar with the HTTPS protocol we have a great guide at the [Javalin website](https://javalin.io/tutorials/javalin-ssl-tutorial).

## Getting started

As simple as adding the dependency:

### Maven

```xml
<dependency>
  <groupId>io.javalin.community.ssl</groupId>
  <artifactId>ssl-plugin</artifactId>
  <version>${javalin.version}</version>
</dependency>
```

### Gradle

```kotlin
implementation("io.javalin.community.ssl:ssl-plugin:$javalinVersion")
```

## Configuration

You can pass a config object when registering the plugin

### Java

```java
Javalin.create(config->{
    ...  // your Javalin config here
    config.plugins.register(new SslPlugin(ssl->{
        ... // your SSL configuration here
        ssl.pemFromPath("/path/to/cert.pem","/path/to/key.pem");
    }));
});
```

### Kotlin

```kotlin
Javalin.create { config ->
    ... // your Javalin config here
    config.registerPlugin(SslPlugin{
        ... // your SSL configuration here
        it.pemFromPath("/path/to/cert.pem", "/path/to/key.pem")
    }
}
```

### Available config options

```kotlin

// Connection options
host=null;                                                            // Host to bind to, by default it will bind to all interfaces
insecure=true;                                                        // Toggle the default http (insecure) connector
secure=true;                                                          // Toggle the default https (secure) connector
http2=true;                                                           // Toggle HTTP/2 Support
http3=false;                                                          // Toggle HTTP/3 Support

securePort=443;                                                       // Port to use on the SSL (secure) connector (TCP)
insecurePort=80;                                                      // Port to use on the http (insecure) connector (TCP)
http3Port=443;                                                        // Port to use on the http3 connector (UDP)
redirect=false;                                                       // Redirect all http requests to https
disableHttp3Upgrade=false;                                            // Disable the HTTP/3 upgrade header 



sniHostCheck=true;                                                    // Enable SNI hostname verification
tlsConfig=TlsConfig.INTERMEDIATE;                                     // Set the TLS configuration. (by default Mozilla's intermediate)

// PEM loading options (mutually exclusive)
pemFromPath("/path/to/cert.pem","/path/to/key.pem");                  // load from the given paths
pemFromPath("/path/to/cert.pem","/path/to/key.pem","keyPassword");    // load from the given paths with the given key password
pemFromClasspath("certName.pem","keyName.pem");                       // load from the given paths in the classpath
pemFromClasspath("certName.pem","keyName.pem","keyPassword");         // load from the given paths in the classpath with the given key password
pemFromInputStream(certInputStream,keyInputStream);                   // load from the given input streams
pemFromInputStream(certInputStream,keyInputStream,"keyPassword");     // load from the given input streams with the given key password
pemFromString(certString,keyString);                                  // load from the given strings
pemFromString(certString,keyString,"keyPassword");                    // load from the given strings with the given key password

// Keystore loading options (PKCS#12/JKS) (mutually exclusive)
keystoreFromPath("/path/to/keystore.jks","keystorePassword");         // load the keystore from the given path
keystoreFromClasspath("keyStoreName.p12","keystorePassword");         // load the keystore from the given path in the classpath
keystoreFromInputStream(keystoreInputStream,"keystorePassword");      // load the keystore from the given input stream

// Advanced options
configConnectors { con -> con.dump() }                                // Set a Consumer to configure the connectors
securityProvider = null;                                              // Use a custom security provider
withTrustConfig { trust -> trust.pemFromString("cert") }              // Set the trust configuration, explained below.
```

#### Trust Configuration

If you want to verify the client certificates (such as mTLS) you can set the trust configuration using the `TrustConfig` class.
In contrast to the identity configuration, you can load multiple certificates from different sources.

By adding a `TrustConfig` to the `SslPlugin` you will enable client certificate verification.

```java
config.plugins.register(new SslPlugin(ssl->{
    ssl.pemFromPath("/path/to/cert.pem","/path/to/key.pem"); // Load our identity data
    // Load the client/CA certificate(s)
    ssl.withTrustConfig(trust->{
        trust.certificateFromPath("/path/to/clientCert.pem");
        trust.certificateFromClasspath("rootCA.pem");
    });
}));
```

```kotlin
// Certificate loading options (PEM/DER/P7B)
certificateFromPath("path/to/certificate.pem");              // load a PEM/DER/P7B cert from the given path
certificateFromClasspath("certificateName.pem");             // load a PEM/DER/P7B cert from the given path in the classpath
certificateFromInputStream(inputStream);                     // load a PEM/DER/P7B cert from the given input stream
p7bCertificateFromString("p7b encoded certificate");         // load a P7B cert from the given string
pemFromString("pem encoded certificate");                    // load a PEM cert from the given string

// Trust store loading options (JKS/PKCS12)
trustStoreFromPath("path/to/truststore.jks", "password");    // load a trust store from the given path
trustStoreFromClasspath("truststore.jks", "password");       // load a trust store from the given path in the classpath
trustStoreFromInputStream(inputStream, "password");          // load a trust store from the given input stream
```

#### Hot reloading

Certificate reloading is supported, if you want to replace the certificate you can simply call `SslPlugin.reload()` with the new configuration.

```kotlin
// Create the plugin outside the Javalin config to hold a reference to reload it
val sslPlugin = SslPlugin { 
    it.loadPemFromPath("/path/to/cert.pem","/path/to/key.pem");
    it.insecurePort = 8080; // any other config you want to change
}

Javalin.create {
    ...  // your Javalin config here
    it.registerPlugin(sslPlugin)
}

// later on, when you want to replace the certificate
sslPlugin.reload {
    // any options other than loading certificates/keys will be ignored.
    it.pemFromPath("/path/to/new/cert.pem","/path/to/new/key.pem");

    // you can also replace trust configuration
    it.withTrustConfig{ trust ->
        trust.certificateFromPath("path/to/new/certificate.pem");
    }
}
```

## Notes

* HTTP/2 **can** be used over an insecure connection.
* If Jetty responds with an `HTTP ERROR 400 Invalid SNI`, you can disable SNI verification by
  setting `sniHostCheck = false`.
* Minimizing your jar can lead to issues, [more info](https://github.com/javalin/javalin-ssl/issues/59).

## Depends on

| Package                                                                 | License                                                                                                              |
|-------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| [Javalin](https://github.com/javalin/javalin)                           | [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) |
| [SSLContext Kickstart](https://github.com/Hakky54/sslcontext-kickstart) | [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) |

## Contributing

Contributions are welcome! Open an issue or pull request if you have a suggestion or bug report.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details
