/*
 * Javalin - https://javalin.io
 * Copyright 2019 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.metrics

import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.jetty.JettyServerThreadPoolMetrics
import io.micrometer.core.instrument.binder.jetty.JettyStatisticsMetrics
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.StatisticsHandler

object JavalinMicrometer {

    private val registry: CompositeMeterRegistry = Metrics.globalRegistry

    @JvmStatic
    fun init(server: Server) {
        Util.ensureDependencyPresent(OptionalDependency.MICROMETER)
        val statisticsHandler = StatisticsHandler()
        server.insertHandler(statisticsHandler)
        JettyStatisticsMetrics.monitor(this.registry, statisticsHandler)
        val threadPoolMetrics = JettyServerThreadPoolMetrics(server.threadPool, emptyList<Tag>())
        threadPoolMetrics.bindTo(this.registry)
    }

}
