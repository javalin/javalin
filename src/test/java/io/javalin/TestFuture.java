package io.javalin;

import io.javalin.newutil.BaseTest;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestFuture extends BaseTest {

    @Test
    public void testFutures() throws Exception {
        app.get("/test-future", ctx -> ctx.result(getFuture("Result")));
        assertThat(http.getBody("/test-future"), is("Result"));
    }

    @Test
    public void testFutures_afterHandler() throws Exception {
        app.get("/test-future", ctx -> ctx.result(getFuture("Not result")));
        app.after(ctx -> ctx.result("Overwritten by after-handler"));
        assertThat(http.getBody("/test-future"), is("Overwritten by after-handler"));
    }

    @Test
    public void testFutures_afterHandler_throwsExceptionForFuture() throws Exception {
        app.get("/test-future", ctx -> ctx.result(getFuture("Not result")));
        app.after("/test-future", ctx -> ctx.result(getFuture("Overwritten by after-handler")));
        assertThat(http.getBody("/test-future"), is("Internal server error"));
    }

    @Test
    public void testFutures_errorHandler() throws Exception {
        app.get("/test-future", ctx -> ctx.result(getFuture("Not result")).status(555));
        app.error(555, ctx -> ctx.result("Overwritten by error-handler"));
        assertThat(http.getBody("/test-future"), is("Overwritten by error-handler"));
    }

    @Test
    public void testFutures_exceptionalFutures_unmapped() throws Exception {
        app.get("/test-future", ctx -> ctx.result(getFuture(null)));
        assertThat(http.getBody("/test-future"), is("Internal server error"));
    }

    @Test
    public void testFutures_exceptionalFutures_mapped() throws Exception {
        app.get("/test-future", ctx -> ctx.result(getFuture(null)));
        app.exception(CancellationException.class, (e, ctx) -> ctx.result("Handled"));
        assertThat(http.getBody("/test-future"), is("Handled"));
    }

    @Test
    public void testFutures_futureInExceptionHandler_throwsException() throws Exception {
        app.get("/test-future", ctx -> {
            throw new Exception();
        });
        app.exception(Exception.class, (exception, ctx) -> ctx.result(getFuture("Exception result")));
        assertThat(http.getBody("/test-future"), is(""));
        assertThat(http.get("/test-future").code(), is(500));
    }

    @Test
    public void testFutures_clearedOnNewResult() throws Exception {
        app.get("/test-future", ctx -> {
            ctx.result(getFuture("Result"));
            ctx.result("Overridden");
        });
        assertThat(http.getBody("/test-future"), is("Overridden"));
    }

    private static CompletableFuture<String> getFuture(String result) {
        CompletableFuture<String> future = new CompletableFuture<>();
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            if (result != null) {
                future.complete(result);
            } else {
                future.cancel(false);
            }
        }, 10, TimeUnit.MILLISECONDS);
        return future;
    }

}
