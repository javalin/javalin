package io.javalin.serversentevent;

@FunctionalInterface
public interface SSEConnect {
    void handler(EventSource eventSource);
}
