package io.javalin;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;

public class TestFuture extends _UnirestBaseTest {

    @Test
    public void testFutures() throws Exception {
        app.get("test-future", ctx -> {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                }
                ctx.result("test");
            });
            ctx.result(future);
        });

        assertEquals("test", GET_asString("/test-future").getBody());
    }
}
