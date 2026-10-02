package io.javalin.dev.maven;

import io.javalin.dev.log.JavalinDevLogger;
import org.apache.maven.plugin.logging.Log;

final class MavenDevLogger implements JavalinDevLogger {
    private final Log log;

    MavenDevLogger(Log log) {
        this.log = log;
    }

    @Override
    public void info(String message) {
        log.info(message);
    }

    @Override
    public void warn(String message) {
        log.warn(message);
    }

    @Override
    public void error(String message) {
        log.error(message);
    }

    @Override
    public void debug(String message) {
        log.debug(message);
    }
}
