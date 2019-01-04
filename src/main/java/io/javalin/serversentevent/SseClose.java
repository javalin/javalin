package io.javalin.serversentevent;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface SseClose {
    void handle(@NotNull EventSource eventSource);
}
