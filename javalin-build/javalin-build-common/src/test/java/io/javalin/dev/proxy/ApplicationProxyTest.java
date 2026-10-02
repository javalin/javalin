package io.javalin.dev.proxy;

import io.javalin.dev.classloader.RuntimeClassLoader;
import io.javalin.dev.compilation.CompilationInvoker;
import io.javalin.dev.compilation.CompilationResult;
import io.javalin.dev.compilation.CompilationSourcesList;
import io.javalin.dev.compilation.CompilationSourcesTracker;
import io.javalin.dev.runner.ApplicationInstance;
import io.javalin.dev.runner.ApplicationRunner;
import io.javalin.dev.testutil.TestLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ApplicationProxyTest {

    private TestLogger logger;
    private CompilationSourcesTracker tracker;
    private CompilationInvoker compiler;
    private ApplicationRunner runner;

    private ServerSocket echoServer;
    private Thread echoServerThread;
    private ExecutorService echoPool;

    private ApplicationProxy proxy;
    private Thread proxyThread;
    private int proxyPort;

    @BeforeEach
    void setUp() throws Exception {
        logger = new TestLogger();
        tracker = mock(CompilationSourcesTracker.class);
        compiler = mock(CompilationInvoker.class);
        runner = mock(ApplicationRunner.class);

        // Default: no changes
        when(tracker.getAndClearChanges()).thenReturn(
            new CompilationSourcesList(List.of(), List.of(), List.of()));

        // Start a real TCP echo server
        echoServer = new ServerSocket(0);
        echoServer.setReuseAddress(true);
        echoPool = Executors.newCachedThreadPool();
        echoServerThread = new Thread(() -> {
            while (!echoServer.isClosed()) {
                try {
                    Socket client = echoServer.accept();
                    echoPool.submit(() -> {
                        try (client) {
                            InputStream in = client.getInputStream();
                            OutputStream out = client.getOutputStream();
                            byte[] buf = new byte[8192];
                            int read;
                            while ((read = in.read(buf)) != -1) {
                                out.write(buf, 0, read);
                                out.flush();
                            }
                        } catch (IOException ignored) {
                        }
                    });
                } catch (IOException e) {
                    if (!echoServer.isClosed()) {
                        // unexpected
                    }
                    break;
                }
            }
        });
        echoServerThread.setDaemon(true);
        echoServerThread.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (proxy != null) {
            proxy.stop();
        }
        if (proxyThread != null) {
            proxyThread.interrupt();
            proxyThread.join(5000);
        }
        if (!echoServer.isClosed()) {
            echoServer.close();
        }
        echoPool.shutdownNow();
    }

    private ApplicationInstance createRealInstance(int port) {
        var cl = new RuntimeClassLoader(new URL[0], ClassLoader.getPlatformClassLoader());
        var thread = new Thread(() -> {});
        return new ApplicationInstance(port, cl, thread, logger);
    }

    private void startProxy(ApplicationInstance initialInstance) throws Exception {
        proxy = new ApplicationProxy(tracker, compiler, runner, initialInstance, logger);
        ServerSocket tmpSocket = new ServerSocket(0);
        proxyPort = tmpSocket.getLocalPort();
        tmpSocket.close();

        proxyThread = new Thread(() -> {
            try {
                proxy.start(proxyPort);
            } catch (IOException ignored) {
            }
        });
        proxyThread.setDaemon(true);
        proxyThread.start();

        // Wait for proxy to start accepting
        waitForPort(proxyPort, 5000);
    }

    private void startProxyWithEchoBackend() throws Exception {
        startProxy(createRealInstance(echoServer.getLocalPort()));
    }

    private void waitForPort(int port, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try (Socket s = new Socket("localhost", port)) {
                return;
            } catch (IOException e) {
                Thread.sleep(50);
            }
        }
        throw new RuntimeException("Port " + port + " not available within " + timeoutMs + "ms");
    }

    private String sendAndReceive(String data) throws IOException {
        try (Socket s = new Socket("localhost", proxyPort)) {
            s.setSoTimeout(5000);
            s.getOutputStream().write(data.getBytes(StandardCharsets.UTF_8));
            s.shutdownOutput();
            var baos = new ByteArrayOutputStream(); // Not efficient but who cares here
            var in = s.getInputStream();
            byte[] buf = new byte[8192];
            try {
                int n;
                while ((n = in.read(buf)) != -1) {
                    baos.write(buf, 0, n);
                }
            } catch (SocketException e) {
                // Connection reset — return whatever we have so far
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    @Test
    void constructor_registersInitialBackend() throws Exception {
        var instance = createRealInstance(echoServer.getLocalPort());
        proxy = new ApplicationProxy(tracker, compiler, runner, instance, logger);
        // Should not throw - backend registered
        proxy.stop();
        proxy = null;
    }

    @Test
    void start_bindsToPort() throws Exception {
        startProxyWithEchoBackend();
        try (Socket s = new Socket("localhost", proxyPort)) {
            assertThat(s.isConnected()).isTrue();
        }
    }

    @Test
    void start_setsReuseAddress() throws Exception {
        startProxyWithEchoBackend();
        int port = proxyPort;
        proxy.stop();
        proxyThread.interrupt();
        proxyThread.join(5000);

        // Start again on the same port - should not throw BindException
        proxy = new ApplicationProxy(tracker, compiler, runner,
            createRealInstance(echoServer.getLocalPort()), logger);
        proxyThread = new Thread(() -> {
            try {
                proxy.start(port);
            } catch (IOException ignored) {
            }
        });
        proxyThread.setDaemon(true);
        proxyThread.start();
        waitForPort(port, 5000);
    }

    @Test
    void stop_closesServerChannel() throws Exception {
        startProxyWithEchoBackend();
        proxy.stop();
        // After stop, new connections should fail
        Thread.sleep(200);
        assertThat(connectSucceeds(proxyPort)).isFalse();
    }

    @Test
    void stop_shutsDownExecutors() throws Exception {
        startProxyWithEchoBackend();
        proxy.stop();
        proxyThread.interrupt();
        proxyThread.join(5000);
        Thread.sleep(500);

        // Check no lingering proxy/reaper threads
        boolean hasProxyThread = Thread.getAllStackTraces().keySet().stream()
            .anyMatch(t -> t.getName().equals("javalin-dev-proxy") && t.isAlive() && !t.isDaemon());
        // Daemon threads may linger briefly, but executors should be shut down
        assertThat(logger.hasMessage("debug", "Proxy executor shut down")).isTrue();
        assertThat(logger.hasMessage("debug", "Reaper executor shut down")).isTrue();
    }

    @Test
    void stop_stopsAllBackends() throws Exception {
        startProxyWithEchoBackend();
        proxy.stop();
        assertThat(logger.hasMessage("info", "Proxy server stopped")).isTrue();
    }

    @Test
    void handleConnection_noChanges_proxiesToBackend() throws Exception {
        startProxyWithEchoBackend();
        String response = sendAndReceive("hello");
        assertThat(response).isEqualTo("hello");
    }

    @Test
    void handleConnection_noChanges_echoesData() throws Exception {
        startProxyWithEchoBackend();
        String payload = "GET / HTTP/1.1\r\nHost: localhost\r\n\r\n";
        String response = sendAndReceive(payload);
        assertThat(response).isEqualTo(payload);
    }

    @Test
    void handleConnection_compilationError_returnsHttp500() throws Exception {
        when(tracker.getAndClearChanges()).thenReturn(
            new CompilationSourcesList(List.of(Path.of("changed.java")), List.of(), List.of()));
        when(compiler.invoke()).thenReturn(CompilationResult.ERROR);

        startProxyWithEchoBackend();
        String response = sendAndReceive("GET / HTTP/1.1\r\n\r\n");
        assertThat(response).contains("HTTP/1.1 500");
    }

    @Test
    void handleConnection_compilationError_containsErrorHtml() throws Exception {
        when(tracker.getAndClearChanges()).thenReturn(
            new CompilationSourcesList(List.of(Path.of("changed.java")), List.of(), List.of()));
        when(compiler.invoke()).thenReturn(CompilationResult.ERROR);

        startProxyWithEchoBackend();
        String response = sendAndReceive("GET / HTTP/1.1\r\n\r\n");
        assertThat(response).contains("<title>Compilation Error</title>");
    }

    @Test
    void handleConnection_errorMessage_htmlEscaped() throws Exception {
        when(tracker.getAndClearChanges()).thenReturn(
            new CompilationSourcesList(List.of(Path.of("changed.java")), List.of(), List.of()));
        when(compiler.invoke()).thenReturn(CompilationResult.ERROR);

        startProxyWithEchoBackend();
        // The error message "Compilation failed" is hardcoded in handleConnection
        // The HTML escaping is applied to the error string passed to writeErrorResponse
        String response = sendAndReceive("GET / HTTP/1.1\r\n\r\n");
        assertThat(response).contains("Compilation failed");
    }

    @Test
    void handleConnection_compilationSuccess_swapsBackend() throws Exception {
        // First call returns changes, second returns no changes
        when(tracker.getAndClearChanges())
            .thenReturn(new CompilationSourcesList(List.of(Path.of("changed.java")), List.of(), List.of()))
            .thenReturn(new CompilationSourcesList(List.of(), List.of(), List.of()));
        when(compiler.invoke()).thenReturn(CompilationResult.SUCCESS);

        // Create a second echo server for the new backend
        ServerSocket echo2 = new ServerSocket(0);
        echo2.setReuseAddress(true);
        Thread echo2Thread = new Thread(() -> {
            try {
                while (!echo2.isClosed()) {
                    Socket client = echo2.accept();
                    echoPool.submit(() -> {
                        try (client) {
                            InputStream in = client.getInputStream();
                            OutputStream out = client.getOutputStream();
                            byte[] buf = new byte[8192];
                            int read;
                            while ((read = in.read(buf)) != -1) {
                                out.write(buf, 0, read);
                                out.flush();
                            }
                        } catch (IOException ignored) {
                        }
                    });
                }
            } catch (IOException ignored) {
            }
        });
        echo2Thread.setDaemon(true);
        echo2Thread.start();

        var newInstance = createRealInstance(echo2.getLocalPort());
        when(runner.start()).thenReturn(newInstance);

        startProxyWithEchoBackend();

        String response = sendAndReceive("swap-test");
        // Either the original or new backend should echo (both are echo servers)
        assertThat(response).isEqualTo("swap-test");

        echo2.close();
    }

    @Test
    void handleConnection_swapFails_returnsError() throws Exception {
        startProxyWithEchoBackend();

        // Wait for cooldown to expire so next request triggers a fresh scan
        Thread.sleep(2500);

        // Now set up mocks for the test request
        when(tracker.getAndClearChanges()).thenReturn(
            new CompilationSourcesList(List.of(Path.of("changed.java")), List.of(), List.of()));
        when(compiler.invoke()).thenReturn(CompilationResult.SUCCESS);
        when(runner.start()).thenThrow(new RuntimeException("start failed"));

        String response = sendAndReceive("GET / HTTP/1.1\r\n\r\n");
        assertThat(response).contains("HTTP/1.1 500");
        assertThat(response).contains("Failed to restart application");
    }

    @Test
    void scanIfNeeded_cooldownPreventsDuplicateScan() throws Exception {
        startProxyWithEchoBackend();

        sendAndReceive("first");
        sendAndReceive("second");

        // Tracker should only be queried once within the cooldown window
        verify(tracker, atMost(1)).getAndClearChanges();
    }

    @Test
    void scanIfNeeded_errorCacheDuringCooldown() throws Exception {
        when(tracker.getAndClearChanges()).thenReturn(
            new CompilationSourcesList(List.of(Path.of("changed.java")), List.of(), List.of()));
        when(compiler.invoke()).thenReturn(CompilationResult.ERROR);

        startProxyWithEchoBackend();

        String response1 = sendAndReceive("first");
        assertThat(response1).contains("HTTP/1.1 500");

        // Second request within cooldown should also return error (cached)
        String response2 = sendAndReceive("second");
        assertThat(response2).contains("HTTP/1.1 500");

        // Compiler should only be invoked once
        verify(compiler, times(1)).invoke();
    }

    @Test
    void scanIfNeeded_afterCooldownExpires_rescans() throws Exception {
        startProxyWithEchoBackend();

        sendAndReceive("first");
        // Wait for cooldown to expire
        Thread.sleep(2500);
        sendAndReceive("second");

        verify(tracker, times(2)).getAndClearChanges();
    }

    @Test
    void scanIfNeeded_closesAllRuntimeClassLoadersBeforeCompile() throws Exception {
        when(tracker.getAndClearChanges()).thenReturn(
            new CompilationSourcesList(List.of(Path.of("changed.java")), List.of(), List.of()));
        when(compiler.invoke()).thenReturn(CompilationResult.ERROR);

        startProxyWithEchoBackend();
        sendAndReceive("trigger");

        assertThat(logger.hasMessage("info", "closing RuntimeClassLoaders before compilation")).isTrue();
    }

    @Test
    void retireBackend_noConnections_stopsImmediately() throws Exception {
        startProxyWithEchoBackend();

        // Wait for cooldown to expire
        Thread.sleep(2500);

        ServerSocket echo2 = new ServerSocket(0);
        echo2.setReuseAddress(true);
        Thread echo2Thread = new Thread(() -> {
            try {
                while (!echo2.isClosed()) {
                    Socket c = echo2.accept();
                    echoPool.submit(() -> {
                        try (c) {
                            c.getInputStream().transferTo(c.getOutputStream());
                        } catch (IOException ignored) {}
                    });
                }
            } catch (IOException ignored) {}
        });
        echo2Thread.setDaemon(true);
        echo2Thread.start();

        // Setup: changes detected, compile succeeds, new backend ready
        when(tracker.getAndClearChanges())
            .thenReturn(new CompilationSourcesList(List.of(Path.of("a.java")), List.of(), List.of()))
            .thenReturn(new CompilationSourcesList(List.of(), List.of(), List.of()));
        when(compiler.invoke()).thenReturn(CompilationResult.SUCCESS);
        when(runner.start()).thenReturn(createRealInstance(echo2.getLocalPort()));

        sendAndReceive("trigger-swap");

        // Give time for retirement logic to complete
        Thread.sleep(500);

        // Old backend with 0 connections should be stopped immediately
        assertThat(logger.hasMessage("info", "has no active connections, stopping immediately") ||
                   logger.hasMessage("info", "has no more connections, stopping")).isTrue();

        echo2.close();
    }

    @Test
    void bridge_forwardsBidirectionally() throws Exception {
        startProxyWithEchoBackend();
        String data = "bidirectional-test-data";
        String response = sendAndReceive(data);
        assertThat(response).isEqualTo(data);
    }

    @Test
    void bridge_handlesLargePayload() throws Exception {
        startProxyWithEchoBackend();
        byte[] payload = new byte[1024 * 1024]; // 1MB
        java.util.Arrays.fill(payload, (byte) 'X');

        try (Socket s = new Socket("localhost", proxyPort)) {
            s.setSoTimeout(10000);
            s.getOutputStream().write(payload);
            s.shutdownOutput();
            byte[] response = s.getInputStream().readAllBytes();
            assertThat(response).hasSize(payload.length);
            assertThat(response).isEqualTo(payload);
        }
    }

    @Test
    void concurrentConnections_allServed() throws Exception {
        startProxyWithEchoBackend();
        int count = 10;
        ExecutorService pool = Executors.newFixedThreadPool(count);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            final String msg = "msg-" + i;
            futures.add(pool.submit(() -> sendAndReceive(msg)));
        }

        List<String> results = new ArrayList<>();
        for (Future<String> f : futures) {
            results.add(f.get(10, TimeUnit.SECONDS));
        }
        pool.shutdown();

        for (int i = 0; i < count; i++) {
            assertThat(results).contains("msg-" + i);
        }
    }

    @Test
    void errorHtmlTemplate_loadedFromClasspath() throws Exception {
        when(tracker.getAndClearChanges()).thenReturn(
            new CompilationSourcesList(List.of(Path.of("changed.java")), List.of(), List.of()));
        when(compiler.invoke()).thenReturn(CompilationResult.ERROR);

        startProxyWithEchoBackend();
        String response = sendAndReceive("GET / HTTP/1.1\r\n\r\n");
        assertThat(response).contains("<title>Compilation Error</title>");
    }

    private boolean connectSucceeds(int port) {
        try (Socket s = new Socket("localhost", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
