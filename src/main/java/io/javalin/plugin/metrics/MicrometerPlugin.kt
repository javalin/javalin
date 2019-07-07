package io.javalin.plugin.metrics

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.jetty.JettyServerThreadPoolMetrics
import io.micrometer.core.instrument.binder.jetty.JettyStatisticsMetrics
import org.eclipse.jetty.server.handler.StatisticsHandler

class MicrometerPlugin @JvmOverloads constructor(val registry: MeterRegistry = Metrics.globalRegistry) : Plugin {
    override fun apply(app: Javalin) {
        app.server()?.server()?.let { server ->
            Util.ensureDependencyPresent(OptionalDependency.MICROMETER)
            val statisticsHandler = StatisticsHandler()
            server.insertHandler(statisticsHandler)
            JettyStatisticsMetrics.monitor(this.registry, statisticsHandler)
            val threadPoolMetrics = JettyServerThreadPoolMetrics(server.threadPool, emptyList<Tag>())
            threadPoolMetrics.bindTo(this.registry)
        }
    }
}
