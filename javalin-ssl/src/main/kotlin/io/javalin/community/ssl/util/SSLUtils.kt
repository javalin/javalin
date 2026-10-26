package io.javalin.community.ssl.util

import io.javalin.community.ssl.SslConfig
import io.javalin.community.ssl.SslConfigException
import io.javalin.community.ssl.TrustConfig
import nl.altindag.ssl.SSLFactory
import nl.altindag.ssl.jetty.util.JettySslUtils
import org.eclipse.jetty.util.ssl.SslContextFactory
import java.security.KeyStore
import java.util.function.Consumer

/**
 * Utility class for SSL related tasks.
 */
object SSLUtils {
    /**
     * Helper method to create a [SslContextFactory] from the SSLFactory.
     * This method is used to create the SSLContextFactory for the Jetty server as well as
     * configure the resulting factory.
     *
     * @param sslFactory The [SSLFactory] to use.
     * @return The created [SslContextFactory].
     */
    fun createSslContextFactory(sslFactory: SSLFactory?): SslContextFactory.Server {
        return JettySslUtils.forServer(sslFactory)
    }

    /**
     * Helper method to create a [SSLFactory] from the given config.
     *
     * @param config The config to use.
     * @return The created [SSLFactory].
     */
    fun getSslFactory(config: SslConfig): SSLFactory {
        return getSslFactory(config, false)
    }

    /**
     * Helper method to create a [SSLFactory] from the given config.
     *
     * @param config    The config to use.
     * @param reloading Whether the SSLFactory is being reloaded or is the first time.
     * @return The created [SSLFactory].
     */
    fun getSslFactory(config: SslConfig, reloading: Boolean): SSLFactory {
        val builder = SSLFactory.builder()

        //Add the identity information
        parseIdentity(config, builder)

        //Add the trust information
        config.trustConfig?.let {
            parseTrust(it, builder)
            builder.withNeedClientAuthentication()
        }

        if (!reloading) {
            builder.withSwappableIdentityMaterial()
            builder.withSwappableTrustMaterial()
            if (config.securityProvider != null) builder.withSecurityProvider(config.securityProvider)
            builder.withCiphers(*config.tlsConfig.cipherSuites)
            builder.withProtocols(*config.tlsConfig.protocols)
        }
        return builder.build()
    }

    /**
     * Helper method to parse the given config and add Identity Material to the given builder.
     *
     * @param config The config to use.
     * @throws SslConfigException if the key configuration is invalid.
     */
    @Throws(SslConfigException::class)
    private fun parseIdentity(config: SslConfig, builder: SSLFactory.Builder) {
        when(config.pvt.loadedIdentity){
            SslConfig.LoadedIdentity.NONE -> throw SslConfigException(SslConfigException.Types.MISSING_CERT_AND_KEY_FILE)
            SslConfig.LoadedIdentity.KEY_MANAGER -> builder.withIdentityMaterial(config.pvt.keyManager)
            SslConfig.LoadedIdentity.KEY_STORE -> builder.withIdentityMaterial(config.pvt.keyStore, config.pvt.identityPassword!!.toCharArray())
        }
    }

    /**
     * Helper method to parse the given config and add Trust Material to the given builder.
     *
     * @param config The config to use.
     */
    private fun parseTrust(config: TrustConfig, builder: SSLFactory.Builder) {
        if (config.certificates.isNotEmpty()) {
            builder.withTrustMaterial(config.certificates)
        }
        if (config.keyStore.isNotEmpty()) {
            config.keyStore.forEach(Consumer { trustStore: KeyStore? -> builder.withTrustMaterial(trustStore) })
        }
    }
}
