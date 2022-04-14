package io.javalin.testing;

public class RunResult {
    public String logs;
    public Exception exception;

    public RunResult(String logs, Exception exception) {
        this.logs = logs;
        this.exception = exception;
    }
}
