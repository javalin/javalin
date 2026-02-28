package io.javalin.dev.runner;

import io.javalin.dev.classloader.RuntimeClassLoader;
import io.javalin.dev.testutil.TestLogger;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationInstanceTest {

    private final TestLogger logger = new TestLogger();

    private ApplicationInstance create(int port, RuntimeClassLoader cl, Thread thread) {
        return new ApplicationInstance(port, cl, thread, logger);
    }

    @Test
    void port_returnsConstructorPort() {
        var cl = new RuntimeClassLoader(new URL[0], ClassLoader.getPlatformClassLoader());
        var instance = create(8080, cl, new Thread(() -> {}));
        assertThat(instance.port()).isEqualTo(8080);
        instance.stop();
    }

    @Test
    void address_returnsLocalhostWithPort() {
        var cl = new RuntimeClassLoader(new URL[0], ClassLoader.getPlatformClassLoader());
        var instance = create(9090, cl, new Thread(() -> {}));
        assertThat(instance.address()).isEqualTo(new InetSocketAddress("localhost", 9090));
        instance.stop();
    }

    @Test
    void stop_interruptsAppThread() throws Exception {
        var cl = new RuntimeClassLoader(new URL[0], ClassLoader.getPlatformClassLoader());
        AtomicBoolean interrupted = new AtomicBoolean(false);
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException e) {
                interrupted.set(true);
            }
        });
        thread.setDaemon(true);
        thread.start();

        var instance = create(8080, cl, thread);
        instance.stop();

        thread.join(5000);
        assertThat(interrupted.get()).isTrue();
    }

    @Test
    void stop_closesRuntimeClassLoader() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        var cl = new RuntimeClassLoader(new URL[0], ClassLoader.getPlatformClassLoader()) {
            @Override
            public void close() throws java.io.IOException {
                closed.set(true);
                super.close();
            }
        };
        var instance = create(8080, cl, new Thread(() -> {}));
        instance.stop();
        assertThat(closed.get()).isTrue();
    }

    @Test
    void stop_isIdempotent() throws Exception {
        AtomicBoolean closeCalled = new AtomicBoolean(false);
        var cl = new RuntimeClassLoader(new URL[0], ClassLoader.getPlatformClassLoader()) {
            @Override
            public void close() throws java.io.IOException {
                if (!closeCalled.compareAndSet(false, true)) {
                    throw new RuntimeException("close called twice");
                }
                super.close();
            }
        };
        var instance = create(8080, cl, new Thread(() -> {}));
        instance.stop();
        instance.stop(); // second stop should be a no-op, no exception
        assertThat(closeCalled.get()).isTrue();
    }

    @Test
    void stop_nullsReferences() throws Exception {
        var cl = new RuntimeClassLoader(new URL[0], ClassLoader.getPlatformClassLoader());
        var instance = create(8080, cl, new Thread(() -> {}));
        instance.stop();
        // After stop, closeRuntimeClassLoader should be a no-op (cl is null internally)
        instance.closeRuntimeClassLoader(); // should not throw
    }

    @Test
    void closeRuntimeClassLoader_closesClassLoader() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        var cl = new RuntimeClassLoader(new URL[0], ClassLoader.getPlatformClassLoader()) {
            @Override
            public void close() throws java.io.IOException {
                closed.set(true);
                super.close();
            }
        };
        var instance = create(8080, cl, new Thread(() -> {}));
        instance.closeRuntimeClassLoader();
        assertThat(closed.get()).isTrue();
        instance.stop();
    }

    @Test
    void closeRuntimeClassLoader_logsWarningOnException() throws Exception {
        var cl = new RuntimeClassLoader(new URL[0], ClassLoader.getPlatformClassLoader()) {
            @Override
            public void close() throws java.io.IOException {
                throw new java.io.IOException("test error");
            }
        };
        var instance = create(8080, cl, new Thread(() -> {}));
        instance.closeRuntimeClassLoader();
        assertThat(logger.hasMessage("warn", "Failed to close RuntimeClassLoader")).isTrue();
        // Need to stop without re-closing the broken classloader
        // stop() will try to close again but that's fine since it handles errors
    }

    @Test
    void stop_withFinishedThread_noException() throws Exception {
        var cl = new RuntimeClassLoader(new URL[0], ClassLoader.getPlatformClassLoader());
        Thread thread = new Thread(() -> {}); // finishes immediately
        thread.start();
        thread.join();

        var instance = create(8080, cl, thread);
        instance.stop(); // should not throw even though thread already finished
    }

    @Test
    void stop_logsMessages() throws Exception {
        var cl = new RuntimeClassLoader(new URL[0], ClassLoader.getPlatformClassLoader());
        var instance = create(8080, cl, new Thread(() -> {}));
        instance.stop();
        assertThat(logger.hasMessage("info", "Stopping ApplicationInstance on port 8080")).isTrue();
    }
}
