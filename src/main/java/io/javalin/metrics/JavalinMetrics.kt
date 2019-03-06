/*
 * Javalin - https://javalin.io
 * Copyright 2019 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.metrics

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.jetty.JettyServerThreadPoolMetrics
import io.micrometer.core.instrument.binder.jetty.JettyStatisticsMetrics
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.eclipse.jetty.util.thread.ThreadPool

class JavalinMetrics private constructor(jettyStatisticsHandler: StatisticsHandler, jettyThreadPool: ThreadPool) {

    val registry: CompositeMeterRegistry = Metrics.globalRegistry

    init {
        registerStatisticsHandler(jettyStatisticsHandler)
        registerThreadPool(jettyThreadPool)
    }

    private fun registerStatisticsHandler(handler: StatisticsHandler) {
        JettyStatisticsMetrics.monitor(this.registry, handler)
    }

    private fun registerThreadPool(threadPool: ThreadPool) {
        val threadPoolMetrics = JettyServerThreadPoolMetrics(threadPool, emptyList<Tag>())
        threadPoolMetrics.bindTo(this.registry)
    }

    companion object {

        private var instance: JavalinMetrics? = null

        /**
         * Creates a new [JavalinMetrics] instance if needed to prevent multiple and unneeded instantiations.
         *
         * @param jettyStatisticsHandler [StatisticsHandler] which is already registered via [org.eclipse.jetty.server.Server.insertHandler]
         * @param jettyThreadPool the [ThreadPool] which is used by [io.javalin.Javalin]
         */
        @Synchronized
        fun createInstanceIfNeeded(jettyStatisticsHandler: StatisticsHandler, jettyThreadPool: ThreadPool) {
            if (instance == null) {
                instance = JavalinMetrics(jettyStatisticsHandler, jettyThreadPool)
            }
        }
    }
}
