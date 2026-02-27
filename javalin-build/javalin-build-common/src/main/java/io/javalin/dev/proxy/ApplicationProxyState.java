package io.javalin.dev.proxy;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

final class ApplicationProxyState {
    private final ApplicationProxyHandle handle;
    private final AtomicLong activeConnections;
    private volatile boolean retired;
    private volatile ScheduledFuture<?> stopTask;

    ApplicationProxyState(ApplicationProxyHandle handle) {
        this.handle = handle;
        this.activeConnections = new AtomicLong(0);
    }

    ApplicationProxyHandle handle() {
        return handle;
    }

    long activeConnections() {
        return activeConnections.get();
    }

    long incrementActiveConnections() {
        return activeConnections.incrementAndGet();
    }

    long decrementActiveConnections() {
        return activeConnections.decrementAndGet();
    }

    boolean isRetired() {
        return retired;
    }

    ApplicationProxyState setRetired(boolean retired) {
        this.retired = retired;
        return this;
    }

    Optional<ScheduledFuture<?>> stopTask() {
        return Optional.ofNullable(stopTask);
    }

    ApplicationProxyState setStopTask(ScheduledFuture<?> forceStop) {
        this.stopTask = forceStop;
        return this;
    }
}
