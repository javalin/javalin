package io.javalin;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.containsString;
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
    public void testFutures_errorMapper() throws Exception {
        app.get("/test-future", ctx -> ctx.result(getFuture("Not result")).status(555));
        app.error(555, ctx -> ctx.result("Overwritten by error-handler"));
        assertThat(GET_asString("/test-future").getBody(), is("Overwritten by error-handler"));
    }

    @Test
    public void testFutures_exceptionalFutures() throws Exception {
        app.get("/test-future", ctx -> ctx.result(getExceptionalFuture()));
        assertThat(GET_asString("/test-future").getBody(), is("Internal server error"));
    }

    @Test
    public void testFutures_throwException_beforeHandler() throws Exception {
        app.before("/test-future", ctx -> ctx.result(getFuture("Not result")));
        assertThat(GET_asString("/test-future").getBody(), containsString("You can only set a future result in an endpoint-handler"));
    }

    @Test
    public void testFutures_throwException_afterHandler() throws Exception {
        app.get("/test-future", ctx -> ctx.result(""));
        app.after(ctx -> ctx.result(getFuture("Overwritten by after-handler")));
        assertThat(GET_asString("/test-future").getBody(), containsString("You can only set a future result in an endpoint-handler"));
    }

    @Test
    public void testFutures_throwException_errorMapper() throws Exception {
        app.get("/test-future", ctx -> ctx.result("").status(555));
        app.error(555, ctx -> ctx.result(getFuture("Overwritten by after-handler")));
        assertThat(GET_asString("/test-future").getBody(), containsString("You can only set a future result in an endpoint-handler"));
    }

    private static CompletableFuture<String> getFuture(String s) {
        CompletableFuture<String> future = new CompletableFuture<>();
        new Timer().schedule(
            new TimerTask() {
                public void run() {
                    future.complete(s);
                }
            },
            100
        );
        return future;
    }

    private static CompletableFuture<String> getExceptionalFuture() {
        CompletableFuture<String> future = new CompletableFuture<>();
        new Timer().schedule(
            new TimerTask() {
                public void run() {
                    future.cancel(false);
                }
            },
            100
        );
        return future;
    }
}
