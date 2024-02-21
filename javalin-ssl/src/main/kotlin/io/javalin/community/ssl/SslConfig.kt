package io.javalin.community.ssl

import nl.altindag.ssl.pem.util.PemUtils
import nl.altindag.ssl.util.KeyStoreUtils
import org.eclipse.jetty.server.ServerConnector
import java.io.InputStream
import java.nio.file.Paths
import java.security.KeyStore
import java.security.Provider
import java.util.function.Consumer
import javax.net.ssl.X509ExtendedKeyManager

/**
 * Data class to hold the configuration for the plugin.
 */

class SslConfig {
    /**
     * Host to bind to.
     */
    @JvmField
    var host: String? = null

    /**
     * Toggle the default http (insecure) connector.
     */
    @JvmField
    var insecure = true

    /**
     * Toggle the default https (secure) connector.
     */
    @JvmField
    var secure = true

    /**
     * Port to use on the SSL (secure) connector.
     */
    @JvmField
    var securePort = 443

    /**
     * Port to use on the http (insecure) connector.
     */
    @JvmField
    var insecurePort = 80

    /**
     * Enable http to https redirection.
     */
    @JvmField
    var redirect = false

    /**
     * Toggle HTTP/2 Support
     */
    @JvmField
    var http2 = true

    /**
     * Disable SNI checks.
     *
     * @see [Configuring SNI](https://www.eclipse.org/jetty/documentation/jetty-11/operations-guide/index.html.og-protocols-ssl-sni)
     */
    @JvmField
    var sniHostCheck = true

    /**
     * TLS Security configuration
     */
    @JvmField
    var tlsConfig: TlsConfig = TlsConfig.INTERMEDIATE

    enum class LoadedIdentity {
        NONE, KEY_MANAGER, KEY_STORE
    }

    /**
     * Internal configuration holder for the identity. DO NOT USE DIRECTLY.
     */
    val pvt = PrivateConfig()

    /**
     * Internal data class to hold the identity configuration. DO NOT USE DIRECTLY.
     */
    class PrivateConfig {

        /**
         * Identity manager to use for the SSLContext.
         * It's meant to be configured using the different loading methods but can be set directly.
         * Exclusive with [keyStore].
         */
        var keyManager : X509ExtendedKeyManager? = null
            set(value) {
                if (loadedIdentity != LoadedIdentity.NONE) {
                    throw SslConfigException(SslConfigException.Types.MULTIPLE_IDENTITY_LOADING_OPTIONS)
                } else if (value != null) {
                    loadedIdentity = LoadedIdentity.KEY_MANAGER
                    field = value
                }
            }

        /**
         * Key store to use for the SSLContext.
         * It's meant to be configured using the different loading methods but can be set directly.
         * Exclusive with [keyManager].
         */
        var keyStore : KeyStore? = null
            set(value) {
                if (loadedIdentity != LoadedIdentity.NONE) {
                    throw SslConfigException(SslConfigException.Types.MULTIPLE_IDENTITY_LOADING_OPTIONS)
                } else if (value != null) {
                    loadedIdentity = LoadedIdentity.KEY_STORE
                    field = value
                }
            }

        var identityPassword: String? = null

        var loadedIdentity: LoadedIdentity = LoadedIdentity.NONE
            private set

    }

    ///////////////////////////////////////////////////////////////
    // PEM Loading Methods
    ///////////////////////////////////////////////////////////////
    /**
     * Load pem formatted identity data from a given path in the system.
     *
     * @param certificatePath path to the certificate chain PEM file.
     * @param privateKeyPath  path to the private key PEM file.
     * @param password        optional password for the private key.
     */
    @JvmOverloads
    fun pemFromPath(certificatePath: String, privateKeyPath: String, password: String? = null) {
        val certPath = Paths.get(certificatePath)
        val keyPath = Paths.get(privateKeyPath)
        pvt.keyManager = password?.let {
            PemUtils.loadIdentityMaterial(certPath, keyPath, password.toCharArray())
        } ?: PemUtils.loadIdentityMaterial(certPath, keyPath)
    }

    /**
     * Load pem formatted identity data from the classpath.
     *
     * @param certificateFile The name of the pem certificate file in the classpath.
     * @param privateKeyFile  The name of the pem private key file in the classpath.
     * @param password        optional password for the private key.
     */
    @JvmOverloads
    fun pemFromClasspath(certificateFile: String, privateKeyFile: String, password: String? = null) {
        pvt.keyManager = password?.let {
            PemUtils.loadIdentityMaterial(certificateFile, privateKeyFile, password.toCharArray())
        } ?: PemUtils.loadIdentityMaterial(certificateFile, privateKeyFile)
    }


    /**
     * Load pem formatted identity data from a given input stream.
     *
     * @param certificateInputStream input stream to the certificate chain PEM file.
     * @param privateKeyInputStream  input stream to the private key PEM file.
     * @param password               optional password for the private key.
     */
    @JvmOverloads
    fun pemFromInputStream(certificateInputStream: InputStream, privateKeyInputStream: InputStream, password: String? = null) {
        pvt.keyManager = password?.let {
            PemUtils.loadIdentityMaterial(certificateInputStream, privateKeyInputStream, password.toCharArray())
        } ?: PemUtils.loadIdentityMaterial(certificateInputStream, privateKeyInputStream)
    }

    /**
     * Load pem formatted identity data from a given string.
     *
     * @param certificateString PEM encoded certificate chain.
     * @param privateKeyString  PEM encoded private key.
     * @param password          optional password for the private key.
     */
    @JvmOverloads
    fun pemFromString(certificateString: String, privateKeyString: String, password: String? = null) {
        pvt.keyManager = PemUtils.parseIdentityMaterial(certificateString, privateKeyString, password?.toCharArray() )
    }

    ///////////////////////////////////////////////////////////////
    // Key Store Loading Methods
    ///////////////////////////////////////////////////////////////
    /**
     * Load a key store from a given path in the system.
     *
     * @param keyStorePath     path to the key store file.
     * @param keyStorePassword password for the key store.
     * @param identityPassword password for the identity, if different from the key store password.
     */
    @JvmOverloads
    fun keystoreFromPath(keyStorePath: String, keyStorePassword: String, identityPassword: String? = null) {
        pvt.keyStore = KeyStoreUtils.loadKeyStore(Paths.get(keyStorePath), keyStorePassword.toCharArray())
        pvt.identityPassword = identityPassword ?: keyStorePassword
    }

    /**
     * Load a key store from a given input stream.
     *
     * @param keyStoreInputStream input stream to the key store file.
     * @param keyStorePassword    password for the key store
     * @param identityPassword password for the identity, if different from the key store password.
     */
    @JvmOverloads
    fun keystoreFromInputStream(keyStoreInputStream: InputStream, keyStorePassword: String, identityPassword: String? = null) {
        pvt.keyStore = KeyStoreUtils.loadKeyStore(keyStoreInputStream, keyStorePassword.toCharArray())
        pvt.identityPassword = identityPassword ?: keyStorePassword
    }

    /**
     * Load a key store from the classpath.
     *
     * @param keyStoreFile     name of the key store file in the classpath.
     * @param keyStorePassword password for the key store.
     * @param identityPassword password for the identity, if different from the key store password.
     */
    @JvmOverloads
    fun keystoreFromClasspath(keyStoreFile: String, keyStorePassword: String, identityPassword: String? = null) {
        pvt.keyStore = KeyStoreUtils.loadKeyStore(keyStoreFile, keyStorePassword.toCharArray())
        pvt.identityPassword = identityPassword ?: keyStorePassword
    }

    ///////////////////////////////////////////////////////////////
    // Advanced Options
    ///////////////////////////////////////////////////////////////
    var configConnectors: Consumer<ServerConnector>? = null
        private set

    /**
     * Consumer to configure the different [ServerConnector] that will be created.
     * This consumer will be called as the last config step for each connector, allowing to override any previous configuration.
     */
    fun configConnectors(configConnectors: Consumer<ServerConnector>?) {
        this.configConnectors = configConnectors
    }

    /**
     * Security provider to use for the SSLContext.
     */
    @JvmField
    var securityProvider: Provider? = null

    ///////////////////////////////////////////////////////////////
    // Trust Store
    ///////////////////////////////////////////////////////////////
    /**
     * Trust store configuration for the server, if not set, every client will be accepted.
     */
    var trustConfig: TrustConfig? = null
        private set

    /**
     * Configure the trust configuration for the server.
     * @param trustConfigConsumer consumer to configure the trust configuration.
     */
    fun withTrustConfig(trustConfigConsumer: Consumer<TrustConfig>) {
        val temp = TrustConfig()
        trustConfigConsumer.accept(temp)
        trustConfig = temp
    }
}
