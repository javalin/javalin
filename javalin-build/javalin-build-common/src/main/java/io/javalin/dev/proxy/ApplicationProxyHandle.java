package io.javalin.dev.proxy;

import io.javalin.dev.runner.ApplicationInstance;

import java.net.InetSocketAddress;

final class ApplicationProxyHandle {
    private final long generation;
    private final ApplicationInstance instance;

    ApplicationProxyHandle(long generation, ApplicationInstance instance) {
        this.generation = generation;
        this.instance = instance;
    }

    long generation() {
        return generation;
    }

    ApplicationInstance instance() {
        return instance;
    }

    InetSocketAddress address() {
        return instance.address();
    }
}
