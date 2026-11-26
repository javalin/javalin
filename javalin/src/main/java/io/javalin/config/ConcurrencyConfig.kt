/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.config

import io.javalin.http.util.AsyncExecutor
import io.javalin.util.ConcurrencyUtil
import io.javalin.util.javalinLazy

class ConcurrencyConfig internal constructor(private val state: JavalinState? = null) {
    constructor() : this(null)
    @JvmField var useVirtualThreads = false
    @JvmField var executor = javalinLazy {
        val executorService = ConcurrencyUtil.executorService("JavalinDefaultAsyncThreadPool", useVirtualThreads)
        state?.registerOwnedExecutor(executorService)
        AsyncExecutor(executorService)
    }
}
