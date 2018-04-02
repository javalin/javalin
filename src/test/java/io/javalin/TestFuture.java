package io.javalin;

import org.junit.Test;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;

public class TestFuture extends _UnirestBaseTest {

    @Test
    public void testFutures() throws Exception {
        app.get("test-future", ctx -> ctx.result(getFuture()));
        assertEquals("Result", GET_asString("/test-future").getBody());
    }

    private static CompletableFuture<String> getFuture() {
        CompletableFuture<String> future = new CompletableFuture<>();
        new Timer().schedule(
                new TimerTask() {
                    public void run() {
                        future.complete("Result");
                    }
                },
                1000
        );
        return future;
    }
}
