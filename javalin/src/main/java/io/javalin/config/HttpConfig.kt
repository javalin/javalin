package io.javalin.config

import io.javalin.http.ContentType
import io.javalin.util.ConcurrencyUtil
import io.javalin.util.javalinLazy
import java.util.function.Supplier

class HttpConfig(private val cfg: JavalinConfig) {
    //@formatter:off
    @JvmField var generateEtags = false
    @JvmField var prefer405over404 = false
    @JvmField var maxRequestSize = 1_000_000L // increase this or use inputstream to handle large requests
    @JvmField var defaultContentType = ContentType.PLAIN
    @JvmField var asyncTimeout = 0L
    private val cachedDefaultAsyncExecutor = javalinLazy { ConcurrencyUtil.executorService("JavalinDefaultAsyncThreadPool", cfg.useLoom) }
    @JvmField var defaultAsyncExecutor = Supplier { cachedDefaultAsyncExecutor.value }
    private val cachedDefaultPooledExecutor = javalinLazy { ConcurrencyUtil.executorService("JavalinDefaultPooledThreadPool", cfg.useLoom) }
    @JvmField var pipedStreamExecutor = Supplier {cachedDefaultPooledExecutor.value }
    //@formatter:on
}
