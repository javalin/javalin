package io.javalin.dev.proxy;

import io.javalin.dev.classloader.RuntimeClassLoader;
import io.javalin.dev.runner.ApplicationInstance;
import io.javalin.dev.testutil.TestLogger;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationProxyHandleTest {

    private final TestLogger logger = new TestLogger();

    private ApplicationInstance createInstance(int port) {
        var cl = new RuntimeClassLoader(new URL[0], ClassLoader.getPlatformClassLoader());
        var thread = new Thread(() -> {});
        return new ApplicationInstance(port, cl, thread, logger);
    }

    @Test
    void generation_returnsValue() {
        var instance = createInstance(8080);
        var handle = new ApplicationProxyHandle(42L, instance);
        assertThat(handle.generation()).isEqualTo(42L);
    }

    @Test
    void instance_returnsValue() {
        var instance = createInstance(8080);
        var handle = new ApplicationProxyHandle(1L, instance);
        assertThat(handle.instance()).isSameAs(instance);
    }

    @Test
    void address_delegatesToInstance() {
        var instance = createInstance(9090);
        var handle = new ApplicationProxyHandle(1L, instance);
        assertThat(handle.address()).isEqualTo(new InetSocketAddress("localhost", 9090));
    }
}
