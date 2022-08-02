package io.javalin.config

import io.javalin.http.ContentType

class HttpConfig {
    //@formatter:off
    @JvmField var generateEtags = false
    @JvmField var prefer405over404 = false
    @JvmField var maxRequestSize = 1_000_000L // increase this or use inputstream to handle large requests
    @JvmField var defaultContentType = ContentType.PLAIN
    @JvmField var asyncTimeout = 0L
    //@formatter:on
}
