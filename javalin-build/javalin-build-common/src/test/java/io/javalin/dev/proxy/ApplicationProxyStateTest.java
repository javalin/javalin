package io.javalin.dev.proxy;

import io.javalin.dev.runner.ApplicationInstance;
import io.javalin.dev.testutil.TestLogger;
import io.javalin.dev.classloader.RuntimeClassLoader;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationProxyStateTest {

    private final TestLogger logger = new TestLogger();

    private ApplicationProxyHandle createHandle(long generation) {
        var instance = createInstanceReflectively(8080);
        return new ApplicationProxyHandle(generation, instance);
    }

    private ApplicationInstance createInstanceReflectively(int port) {
        var cl = new RuntimeClassLoader(new URL[0], ClassLoader.getPlatformClassLoader());
        var thread = new Thread(() -> {});
        return new ApplicationInstance(port, cl, thread, logger);
    }

    @Test
    void handle_returnsValue() {
        var handle = createHandle(1L);
        var state = new ApplicationProxyState(handle);
        assertThat(state.handle()).isSameAs(handle);
    }

    @Test
    void activeConnections_initiallyZero() {
        var state = new ApplicationProxyState(createHandle(1L));
        assertThat(state.activeConnections()).isZero();
    }

    @Test
    void incrementActiveConnections() {
        var state = new ApplicationProxyState(createHandle(1L));
        state.incrementActiveConnections();
        state.incrementActiveConnections();
        state.incrementActiveConnections();
        assertThat(state.activeConnections()).isEqualTo(3);
    }

    @Test
    void decrementActiveConnections() {
        var state = new ApplicationProxyState(createHandle(1L));
        state.incrementActiveConnections();
        state.incrementActiveConnections();
        state.decrementActiveConnections();
        assertThat(state.activeConnections()).isEqualTo(1);
    }

    @Test
    void isRetired_initiallyFalse() {
        var state = new ApplicationProxyState(createHandle(1L));
        assertThat(state.isRetired()).isFalse();
    }

    @Test
    void setRetired_setsFlag() {
        var state = new ApplicationProxyState(createHandle(1L));
        state.setRetired(true);
        assertThat(state.isRetired()).isTrue();
    }

    @Test
    void setRetired_returnsSelf() {
        var state = new ApplicationProxyState(createHandle(1L));
        assertThat(state.setRetired(true)).isSameAs(state);
    }

    @Test
    void stopTask_initiallyEmpty() {
        var state = new ApplicationProxyState(createHandle(1L));
        assertThat(state.stopTask()).isEmpty();
    }

    @Test
    void setStopTask_setsTask() {
        var state = new ApplicationProxyState(createHandle(1L));
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            ScheduledFuture<?> future = scheduler.schedule(() -> {}, 1, TimeUnit.HOURS);
            state.setStopTask(future);
            assertThat(state.stopTask()).isPresent().containsSame(future);
            future.cancel(false);
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void setStopTask_returnsSelf() {
        var state = new ApplicationProxyState(createHandle(1L));
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            ScheduledFuture<?> future = scheduler.schedule(() -> {}, 1, TimeUnit.HOURS);
            assertThat(state.setStopTask(future)).isSameAs(state);
            future.cancel(false);
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void concurrentIncrementDecrement_threadSafe() throws Exception {
        var state = new ApplicationProxyState(createHandle(1L));
        int threadCount = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    state.incrementActiveConnections();
                    state.decrementActiveConnections();
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        pool.shutdown();
        assertThat(errors.get()).isZero();
        assertThat(state.activeConnections()).isZero();
    }
}
