package io.javalin.dev.testutil;

import io.javalin.dev.log.JavalinDevLogger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TestLogger implements JavalinDevLogger {

    public record LogEntry(String level, String message) {}

    private final CopyOnWriteArrayList<LogEntry> entries = new CopyOnWriteArrayList<>();

    @Override
    public void info(String message) {
        entries.add(new LogEntry("info", message));
    }

    @Override
    public void warn(String message) {
        entries.add(new LogEntry("warn", message));
    }

    @Override
    public void error(String message) {
        entries.add(new LogEntry("error", message));
    }

    @Override
    public void debug(String message) {
        entries.add(new LogEntry("debug", message));
    }

    public List<LogEntry> all() {
        return List.copyOf(entries);
    }

    public List<LogEntry> messages(String level) {
        return entries.stream()
            .filter(e -> e.level().equals(level))
            .toList();
    }

    public boolean hasMessage(String level, String substring) {
        return entries.stream()
            .anyMatch(e -> e.level().equals(level) && e.message().contains(substring));
    }

    public void clear() {
        entries.clear();
    }
}
