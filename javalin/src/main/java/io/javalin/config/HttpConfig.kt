package io.javalin.config

import io.javalin.compression.CompressionStrategy
import io.javalin.http.ContentType

/**
 * Configuration for the HTTP layer.
 *
 * @param cfg the parent Javalin Configuration
 * @see [JavalinState.http]
 */
class HttpConfig() {
    //@formatter:off
    @JvmField var generateEtags = false
    @JvmField var prefer405over404 = false
    @JvmField var strictContentTypes = false
    @JvmField var maxRequestSize = 1_000_000L // increase this or use inputstream to handle large requests
    @JvmField var responseBufferSize: Int? = null
    @JvmField var defaultContentType = ContentType.PLAIN
    @JvmField var asyncTimeout = 0L
    @JvmField var compressionStrategy = CompressionStrategy.GZIP
    //@formatter:on

}
