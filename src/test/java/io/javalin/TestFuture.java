package io.javalin;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestFuture extends _UnirestBaseTest {

    @Test
    public void testFutures() throws Exception {
        app.get("/test-future", ctx -> ctx.result(getFuture("Result")));
        assertThat(GET_asString("/test-future").getBody(), is("Result"));
    }

    @Test
    public void testFutures_afterHandler() throws Exception {
        app.get("/test-future", ctx -> ctx.result(getFuture("Not result")));
        app.after(ctx -> ctx.result("Overwritten by after-handler"));
        assertThat(GET_asString("/test-future").getBody(), is("Overwritten by after-handler"));
    }

    @Test
    public void testFutures_afterHandler_throwsExceptionForFuture() throws Exception {
        app.get("/test-future", ctx -> ctx.result(getFuture("Not result")));
        app.after("/test-future", ctx -> ctx.result(getFuture("Overwritten by after-handler")));
        assertThat(GET_asString("/test-future").getBody(), is("Internal server error"));
    }

    @Test
    public void testFutures_errorHandler() throws Exception {
        app.get("/test-future", ctx -> ctx.result(getFuture("Not result")).status(555));
        app.error(555, ctx -> ctx.result("Overwritten by error-handler"));
        assertThat(GET_asString("/test-future").getBody(), is("Overwritten by error-handler"));
    }

    @Test
    public void testFutures_exceptionalFutures() throws Exception {
        app.get("/test-future", ctx -> ctx.result(getFuture(null)));
        assertThat(GET_asString("/test-future").getBody(), is("Internal server error"));
    }

    @Test
    public void testFutures_futureInExceptionHandler_throwsException() throws Exception {
        app.get("/test-future", ctx -> {
            throw new Exception();
        });
        app.exception(Exception.class, (exception, ctx) -> ctx.result(getFuture("Exception result")));
        assertThat(GET_asString("/test-future").getBody(), is(""));
        assertThat(GET_asString("/test-future").getStatus(), is(500));
    }

    @Test
    public void testFutures_clearedOnNewResult() throws Exception {
        app.get("/test-future", ctx -> ctx.result(getFuture("Result")).next());
        app.get("/test-future", ctx -> ctx.result("Overridden"));
        assertThat(GET_asString("/test-future").getBody(), is("Overridden"));
    }

    private static CompletableFuture<String> getFuture(String result) {
        CompletableFuture<String> future = new CompletableFuture<>();
        new Timer().schedule(
            new TimerTask() {
                public void run() {
                    if (result != null) {
                        future.complete(result);
                    } else {
                        future.cancel(false);
                    }
                }
            },
            10
        );
        return future;
    }

}
