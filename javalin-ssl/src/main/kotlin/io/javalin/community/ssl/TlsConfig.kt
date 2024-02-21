package io.javalin.community.ssl

/**
 * Data class for the SSL configuration.
 *
 * @see [Security/Server Side TLS](https://wiki.mozilla.org/Security/Server_Side_TLS)
 */
class TlsConfig(
    /**
     * String array of cipher suites to use, following the guidelines in the [ Jetty documentation](https://www.eclipse.org/jetty/documentation/jetty-11/operations-guide/index.html#og-protocols-ssl-customize-ciphers).
     */
    val cipherSuites: Array<String>,
    /**
     * String array of protocols to use, following the guidelines in the [ Jetty documentation](https://www.eclipse.org/jetty/documentation/jetty-11/operations-guide/index.html#og-protocols-ssl-customize-versions).
     */
    val protocols: Array<String>
) {

    override fun toString(): String {
        return "TlsConfig(cipherSuites=" + cipherSuites.contentDeepToString() + ", protocols=" + protocols.contentDeepToString() + ")"
    }

    companion object {
        private const val GUIDELINES_VERSION = "5.7"

        /**
         * For modern clients that support TLS 1.3, with no need for backwards compatibility
         */
        @JvmField
        val MODERN = TlsConfig(
            arrayOf(
                "TLS_AES_128_GCM_SHA256",
                "TLS_AES_256_GCM_SHA384",
                "TLS_CHACHA20_POLY1305_SHA256"),
            arrayOf("TLSv1.3")
        )
        @Deprecated("Use TlsConfig.MODERN instead", ReplaceWith("TlsConfig.MODERN"))
        fun getMODERN(): TlsConfig = MODERN

        /**
         * Recommended configuration for a general-purpose server
         */
        @JvmField
        val INTERMEDIATE = TlsConfig(
            arrayOf(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256"
            ), arrayOf("TLSv1.3","TLSv1.2")
        )
        @Deprecated("Use TlsConfig.INTERMEDIATE instead", ReplaceWith("TlsConfig.INTERMEDIATE"))
        fun getINTERMEDIATE(): TlsConfig = INTERMEDIATE

        /**
         * For services accessed by very old clients or libraries, such as Internet Explorer 8 (Windows XP), Java 6, or OpenSSL 0.9.8
         */
        @JvmField
        val OLD = TlsConfig(
            arrayOf(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                "TLS_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_RSA_WITH_AES_256_CBC_SHA256",
                "TLS_RSA_WITH_AES_128_CBC_SHA",
                "TLS_RSA_WITH_AES_256_CBC_SHA",
                "TLS_RSA_WITH_3DES_EDE_CBC_SHA"
            ), arrayOf("TLSv1.3","TLSv1.2", "TLSv1.1", "TLSv1")
        )
        @Deprecated("Use TlsConfig.OLD instead", ReplaceWith("TlsConfig.OLD"))
        fun getOLD(): TlsConfig = OLD
    }
}
