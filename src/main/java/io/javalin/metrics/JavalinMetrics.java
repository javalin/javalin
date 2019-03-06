/*
 * Javalin - https://javalin.io
 * Copyright 2019 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.metrics;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jetty.JettyServerThreadPoolMetrics;
import io.micrometer.core.instrument.binder.jetty.JettyStatisticsMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public class JavalinMetrics {

    @Nullable
    private static JavalinMetrics instance;

    /**
     * Creates a new {@link JavalinMetrics} instance if needed to prevent multiple and unneeded instantiations.
     *
     * @param jettyStatisticsHandler {@link StatisticsHandler} which is already registered via {@link org.eclipse.jetty.server.Server#insertHandler(HandlerWrapper)}
     * @param jettyThreadPool the {@link ThreadPool} which is used by {@link io.javalin.Javalin}
     */
    @NotNull
    public synchronized static void createInstanceIfNeeded(@NotNull StatisticsHandler jettyStatisticsHandler, @NotNull ThreadPool jettyThreadPool) {
        if(instance == null) {
            instance = new JavalinMetrics(jettyStatisticsHandler, jettyThreadPool);
        }

    }

    @NotNull
    public final CompositeMeterRegistry registry;

    private JavalinMetrics(@NotNull StatisticsHandler jettyStatisticsHandler, @NotNull ThreadPool jettyThreadPool) {
        registry = Metrics.globalRegistry;
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
