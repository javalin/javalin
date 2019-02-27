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
import java.util.Optional;

public class JavalinMetrics {

    @Nullable
    private static JavalinMetrics instance;

    /**
     * Creates a new {@link JavalinMetrics} instance if needed to prevent multiple and unneeded instantiations.
     *
     * @param jettyStatisticsHandler {@link StatisticsHandler} which is already registered via {@link org.eclipse.jetty.server.Server#insertHandler(HandlerWrapper)}
     * @param jettyThreadPool the {@link ThreadPool} which is used by {@link io.javalin.Javalin}
     * @return new created or already present instance of {@link JavalinMetrics}
     */
    @NotNull
    public synchronized static JavalinMetrics createInstanceIfNeeded(@NotNull StatisticsHandler jettyStatisticsHandler, @NotNull ThreadPool jettyThreadPool) {
        if(instance == null) {
            instance = new JavalinMetrics(jettyStatisticsHandler, jettyThreadPool);
        }

        return instance;
    }

    @NotNull
    public Optional<JavalinMetrics> getInstance() {
        return Optional.ofNullable(instance);
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
