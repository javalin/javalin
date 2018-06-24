package io.javalin.serversentevent;

@FunctionalInterface
public interface SSEClose {
    void handler(EventSource eventSource);
}
