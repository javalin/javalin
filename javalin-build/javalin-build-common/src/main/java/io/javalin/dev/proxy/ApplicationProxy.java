package io.javalin.dev.proxy;

import io.javalin.dev.compilation.CompilationInvoker;
import io.javalin.dev.compilation.CompilationResult;
import io.javalin.dev.compilation.CompilationSourcesList;
import io.javalin.dev.compilation.CompilationSourcesTracker;
import io.javalin.dev.log.JavalinDevLogger;
import io.javalin.dev.runner.ApplicationInstance;
import io.javalin.dev.runner.ApplicationRunner;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class ApplicationProxy {
    private static final Duration MAX_BACKEND_READY_DURATION = Duration.ofSeconds(5);
    private static final Duration MAX_DRAIN_DURATION = Duration.ofSeconds(30);
    private static final long SCAN_COOLDOWN_MS = 2000;
    private static final int BUFFER_SIZE = 65536;
    private static final String ERROR_HTML_TEMPLATE;

    static {
        try (InputStream is = ApplicationProxy.class.getResourceAsStream("/compilation-error.html")) {
            if (is == null) {
                throw new IllegalStateException("Missing resource: compilation-error.html");
            }
            ERROR_HTML_TEMPLATE = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final CompilationSourcesTracker tracker;
    private final CompilationInvoker compiler;
    private final ApplicationRunner runner;
    private final JavalinDevLogger logger;

    private volatile ServerSocketChannel serverChannel;
    private volatile long lastScanTime;
    private volatile CompilationResult lastScanResult;
    private final Object scanLock = new Object();

    private ExecutorService executor;
    private ScheduledExecutorService reaper;

    private final AtomicLong generationSeq = new AtomicLong(0);
    private final AtomicReference<ApplicationProxyHandle> currentBackend = new AtomicReference<>();
    private final ConcurrentHashMap<Long, ApplicationProxyState> backends = new ConcurrentHashMap<>();

    public ApplicationProxy(CompilationSourcesTracker tracker, CompilationInvoker compiler, ApplicationRunner runner, ApplicationInstance initialInstance, JavalinDevLogger logger) {
        this.tracker = tracker;
        this.compiler = compiler;
        this.runner = runner;
        this.logger = logger;
        this.lastScanResult = CompilationResult.NO_CHANGES;

        ApplicationProxyHandle initial = new ApplicationProxyHandle(nextGen(), initialInstance);
        currentBackend.set(initial);
        backends.put(initial.generation(), new ApplicationProxyState(initial));
        logger.debug("ApplicationProxy initialized with initial backend generation " + initial.generation()
            + " on port " + initialInstance.port());
    }

    private long nextGen() {
        return generationSeq.incrementAndGet();
    }

    public boolean start(int port) throws IOException {
        logger.info("Starting proxy server on port " + port);
        serverChannel = ServerSocketChannel.open();
        serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(true);

        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "javalin-dev-proxy");
            t.setDaemon(true);
            return t;
        });

        reaper = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "javalin-dev-reaper");
            t.setDaemon(true);
            return t;
        });

        logger.info("Proxy server listening on port " + port + ", ready to accept connections");

        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    SocketChannel client = serverChannel.accept();
                    client.setOption(StandardSocketOptions.TCP_NODELAY, true);
                    logger.debug("Accepted incoming connection from " + client.getRemoteAddress());
                    executor.submit(() -> handleConnection(client));
                } catch (IOException e) {
                    if (serverChannel == null || !serverChannel.isOpen()) {
                        logger.info("Server channel closed, exiting accept loop");
                        break;
                    }
                    logger.warn("IOException during accept: " + e.getMessage());
                }
            }
        } finally {
            stop();
        }

        return true;
    }

    public void stop() {
        logger.info("Stopping proxy server");
        try {
            if (serverChannel != null && serverChannel.isOpen()) {
                serverChannel.close();
                logger.debug("Server channel closed");
            }
        } catch (IOException e) {
            logger.warn("Error closing server channel: " + e.getMessage());
        }

        if (executor != null) {
            executor.shutdownNow();
            logger.debug("Proxy executor shut down");
        }
        if (reaper != null) {
            reaper.shutdownNow();
            logger.debug("Reaper executor shut down");
        }

        logger.debug("Stopping " + backends.size() + " backend(s)");
        for (ApplicationProxyState state : backends.values()) {
            stopBackend(state);
        }
        backends.clear();
        logger.info("Proxy server stopped");
    }

    private void handleConnection(SocketChannel client) {
        try (client) {
            CompilationResult result = scanIfNeeded();

            if (result == CompilationResult.ERROR) {
                logger.warn("Returning compilation error response to client");
                writeErrorResponse(client, "Compilation failed");
                return;
            }

            if (result == CompilationResult.SUCCESS) {
                logger.info("Source changes compiled successfully, swapping backend");
                if (!startAndSwapBackend()) {
                    logger.error("Failed to start and swap to new backend after successful compilation");
                    writeErrorResponse(client, "Failed to restart application");
                    return;
                }
            }

            ApplicationProxyHandle backend = currentBackend.get();
            ApplicationProxyState state = backends.get(backend.generation());
            if (state == null) {
                logger.error("No backend state found for generation " + backend.generation());
                writeErrorResponse(client, "No backend available");
                return;
            }

            long activeCount = state.incrementActiveConnections();
            logger.debug("Proxying request to backend generation " + backend.generation()
                + " on port " + backend.address().getPort()
                + " (active connections: " + activeCount + ")");
            try {
                Optional<SocketChannel> target = connectToBackend(backend.address());
                if (target.isEmpty()) {
                    logger.error("Cannot connect to backend at " + backend.address());
                    writeErrorResponse(client, "Cannot connect to internal service");
                    return;
                }

                try (SocketChannel delegate = target.get()) {
                    bridge(client, delegate);
                }
            } finally {
                long remaining = state.decrementActiveConnections();
                logger.debug("Connection to generation " + backend.generation() + " completed (remaining: " + remaining + ")");
                if (state.isRetired() && remaining == 0) {
                    logger.info("Retired backend generation " + state.handle().generation() + " has no more connections, stopping");
                    stopBackend(state);
                    backends.remove(state.handle().generation(), state);
                }
            }
        } catch (Exception e) {
            logger.error("Unhandled exception in connection handler: " + e.getMessage());
            try {
                writeErrorResponse(client, e.getMessage());
            } catch (IOException ignored) {
            }
        }
    }

    private CompilationResult scanIfNeeded() {
        synchronized (scanLock) {
            if (System.currentTimeMillis() - lastScanTime < SCAN_COOLDOWN_MS) {
                logger.debug("Scan cooldown active, returning cached result: " + lastScanResult);
                return lastScanResult == CompilationResult.ERROR
                    ? lastScanResult
                    : CompilationResult.NO_CHANGES;
            }

            try {
                CompilationSourcesList changes = tracker.getAndClearChanges();
                if (changes.isEmpty()) {
                    lastScanResult = CompilationResult.NO_CHANGES;
                    return CompilationResult.NO_CHANGES;
                }

                logger.info("Source changes detected, closing RuntimeClassLoaders before compilation");
                // Close ALL RuntimeClassLoaders before compilation (Windows file locking).
                // Backends keep running, but they won't be able to lazily load new classes after close.
                for (ApplicationProxyState s : backends.values()) {
                    try {
                        s.handle().instance().closeRuntimeClassLoader();
                    } catch (Throwable e) {
                        logger.warn("Failed to close RuntimeClassLoader for generation " + s.handle().generation() + " (" + e.getMessage() + ")");
                    }
                }

                logger.info("Invoking compiler...");
                CompilationResult result = compiler.invoke();
                lastScanResult = result;
                logger.info("Compilation result: " + result);
                return result;
            } catch (Exception e) {
                logger.error("Exception during scan/compile: " + e.getMessage());
                lastScanResult = CompilationResult.ERROR;
                return CompilationResult.ERROR;
            } finally {
                lastScanTime = System.currentTimeMillis();
            }
        }
    }

    private boolean startAndSwapBackend() {
        ApplicationInstance nextInstance;
        try {
            logger.info("Starting new backend instance...");
            nextInstance = runner.start();
        } catch (Exception e) {
            logger.error("Failed to start new backend instance: " + e.getMessage());
            return false;
        }

        logger.debug("Waiting for new backend to become ready at " + nextInstance.address());
        if (!awaitBackendReady(nextInstance.address())) {
            logger.error("New backend did not become ready within " + MAX_BACKEND_READY_DURATION.toSeconds() + "s at " + nextInstance.address());
            safeStop(nextInstance);
            return false;
        }

        ApplicationProxyHandle next = new ApplicationProxyHandle(nextGen(), nextInstance);
        backends.put(next.generation(), new ApplicationProxyState(next));
        logger.info("New backend ready: generation " + next.generation() + " on port " + nextInstance.port());

        ApplicationProxyHandle old = currentBackend.getAndSet(next);
        logger.info("Swapped backend: generation " + old.generation() + " -> " + next.generation());

        retireBackend(old);
        return true;
    }

    private void retireBackend(ApplicationProxyHandle old) {
        ApplicationProxyState state = backends.get(old.generation());
        if (state == null) {
            return;
        }

        state.setRetired(true);
        long activeConns = state.activeConnections();
        logger.debug("Retiring backend generation " + old.generation() + " (active connections: " + activeConns + ")");

        if (activeConns == 0) {
            logger.info("Retired backend generation " + old.generation() + " has no active connections, stopping immediately");
            stopBackend(state);
            backends.remove(state.handle().generation(), state);
            return;
        }

        logger.info("Retired backend generation " + old.generation() + " still has " + activeConns
            + " active connection(s), scheduling force-stop in " + MAX_DRAIN_DURATION.toSeconds() + "s");
        if (reaper != null) {
            var task = reaper.schedule(() -> {
                logger.warn("Force-stopping retired backend generation " + old.generation() + " after drain timeout");
                stopBackend(state);
                backends.remove(state.handle().generation(), state);
            }, MAX_DRAIN_DURATION.toMillis(), TimeUnit.MILLISECONDS);
            state.setStopTask(task);
        }
    }

    private void stopBackend(ApplicationProxyState state) {
        logger.debug("Stopping backend generation " + state.handle().generation());
        state.stopTask()
            .ifPresent(task -> task.cancel(false));

        safeStop(state.handle().instance());
    }

    private void safeStop(ApplicationInstance instance) {
        try {
            instance.stop();
        } catch (Throwable e) {
            logger.warn("Error stopping application instance on port " + instance.port() + " (" + e.getMessage() + ")");
        }
    }

    private boolean awaitBackendReady(InetSocketAddress address) {
        var start = Instant.now();
        while (Duration.between(start, Instant.now()).compareTo(MAX_BACKEND_READY_DURATION) < 0) {
            try (SocketChannel ch = SocketChannel.open(address)) {
                ch.setOption(StandardSocketOptions.TCP_NODELAY, true);
                logger.debug("Backend at " + address + " is ready (connected successfully)");
                return true;
            } catch (IOException e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while waiting for backend readiness at " + address);
                    return false;
                }
            }
        }
        logger.warn("Backend at " + address + " did not become ready within " + MAX_BACKEND_READY_DURATION.toSeconds() + "s");
        return false;
    }

    private void bridge(SocketChannel client, SocketChannel target) {
        Thread clientToTarget = new Thread(() -> {
            try {
                transfer(client, target);
            } finally {
                silentShutdown(target);
            }
        }, "javalin-dev-c2t");

        Thread targetToClient = new Thread(() -> {
            try {
                transfer(target, client);
            } finally {
                silentShutdown(client);
            }
        }, "javalin-dev-t2c");

        clientToTarget.setDaemon(true);
        targetToClient.setDaemon(true);
        clientToTarget.start();
        targetToClient.start();

        try {
            clientToTarget.join();
            targetToClient.join();
        } catch (InterruptedException e) {
            clientToTarget.interrupt();
            targetToClient.interrupt();
            Thread.currentThread().interrupt();
        }
    }

    private void transfer(SocketChannel src, SocketChannel dst) {
        ByteBuffer buf = ByteBuffer.allocateDirect(BUFFER_SIZE);
        try {
            while (src.read(buf) != -1) {
                buf.flip();
                while (buf.hasRemaining()) {
                    dst.write(buf);
                }
                buf.clear();
            }
        } catch (IOException ignored) {
        }
    }

    private void writeErrorResponse(SocketChannel client, String errorMessage) throws IOException {
        String escaped = errorMessage != null
            ? errorMessage.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            : "Unknown error";
        String html = ERROR_HTML_TEMPLATE.formatted(escaped);
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        String header = "HTTP/1.1 500 Compilation Error\r\n"
                        + "Content-Type: text/html; charset=utf-8\r\n"
                        + "Content-Length: " + body.length + "\r\n"
                        + "Connection: close\r\n"
                        + "\r\n";

        byte[] headerBytes = header.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buf = ByteBuffer.allocate(headerBytes.length + body.length);
        buf.put(headerBytes);
        buf.put(body);
        buf.flip();
        while (buf.hasRemaining()) {
            client.write(buf);
        }
    }

    private Optional<SocketChannel> connectToBackend(InetSocketAddress address) {
        var start = Instant.now();
        while (Duration.between(start, Instant.now()).compareTo(MAX_BACKEND_READY_DURATION) < 0) {
            SocketChannel ch = null;
            try {
                ch = SocketChannel.open(address);
                ch.setOption(StandardSocketOptions.TCP_NODELAY, true);
                return Optional.of(ch);
            } catch (IOException e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while connecting to backend at " + address);
                    return Optional.empty();
                } finally {
                    silentClose(ch);
                }
            }
        }
        logger.warn("Could not connect to backend at " + address + " within " + MAX_BACKEND_READY_DURATION.toSeconds() + "s");
        return Optional.empty();
    }

    private void silentShutdown(SocketChannel ch) {
        try {
            if (ch != null && ch.isOpen()) {
                ch.shutdownOutput();
            }
        } catch (IOException ignored) {
        }
    }

    private void silentClose(SocketChannel ch) {
        try {
            if (ch != null) {
                ch.close();
            }
        } catch (IOException ignored) {
        }
    }
}
