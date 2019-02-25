package io.javalin.metrics;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jetty.JettyServerThreadPoolMetrics;
import io.micrometer.core.instrument.binder.jetty.JettyStatisticsMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class JavalinMetrics {

    @NotNull
    public final CompositeMeterRegistry registry;

    public JavalinMetrics(@NotNull StatisticsHandler jettyStatisticsHandler, @NotNull ThreadPool jettyThreadPool) {
        registry  = Metrics.globalRegistry;
        registerStatisticsHandler(jettyStatisticsHandler);
        registerThreadPool(jettyThreadPool);
    }

    private void registerStatisticsHandler(@NotNull StatisticsHandler handler) {
        JettyStatisticsMetrics.monitor(this.registry, handler);
    }

    private void registerThreadPool(@NotNull ThreadPool threadPool) {
        JettyServerThreadPoolMetrics threadPoolMetrics = new JettyServerThreadPoolMetrics(threadPool, Collections.emptyList());
        threadPoolMetrics.bindTo(this.registry);
    }
}
