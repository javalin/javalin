package io.javalin.dev.runner;

import io.javalin.dev.classloader.RuntimeClassLoader;
import io.javalin.dev.log.JavalinDevLogger;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ApplicationInstance {
    private final int port;
    private final AtomicBoolean stopped;
    private final JavalinDevLogger logger;

    private volatile RuntimeClassLoader runtimeClassLoader;
    private volatile Thread appThread;

    public ApplicationInstance(int port, RuntimeClassLoader runtimeClassLoader, Thread appThread, JavalinDevLogger logger) {
        this.port = port;
        this.runtimeClassLoader = runtimeClassLoader;
        this.appThread = appThread;
        this.stopped = new AtomicBoolean(false);
        this.logger = logger;
        logger.debug("ApplicationInstance created on port " + port);
    }

    public int port() {
        return port;
    }

    public InetSocketAddress address() {
        return new InetSocketAddress("localhost", port);
    }

    public void closeRuntimeClassLoader() {
        RuntimeClassLoader cl = runtimeClassLoader;
        if (cl != null) {
            try {
                cl.close();
                logger.debug("RuntimeClassLoader closed for instance on port " + port);
            } catch (Exception e) {
                logger.warn("Failed to close RuntimeClassLoader for instance on port " + port + " (" + e.getMessage() + ")");
            }
        }
    }

    public void stop() {
        if (!stopped.compareAndSet(false, true)) {
            logger.debug("ApplicationInstance on port " + port + " already stopped, skipping");
            return;
        }

        logger.info("Stopping ApplicationInstance on port " + port);

        // Also interrupt the launching thread (in case it didn't inherit, or as a fallback)
        Thread t = appThread;
        if (t != null && t.isAlive()) {
            logger.debug("Interrupting application thread: " + t.getName());
            t.interrupt();
        }

        // Release file locks
        closeRuntimeClassLoader();

        // Allow GC after stop
        runtimeClassLoader = null;
        appThread = null;
        logger.debug("ApplicationInstance on port " + port + " stopped and cleaned up");
    }
}
