package io.javalin.dev.log;

public interface JavalinDevLogger {
    void info(String message);
    void warn(String message);
    void error(String message);
    void debug(String message);
}
