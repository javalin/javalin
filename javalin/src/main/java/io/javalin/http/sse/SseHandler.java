package io.javalin.http.sse;

import io.javalin.core.util.Header;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Consumer;

public class SseHandler implements Handler {

    private final Consumer<SseClient> clientConsumer;

    public SseHandler(@NotNull Consumer<SseClient> clientConsumer) {
        this.clientConsumer = clientConsumer;
    }

    @Override
    public void handle(Context ctx) throws IOException {
        if ("text/event-stream".equals(ctx.header(Header.ACCEPT))) {
            final HttpServletResponse res = ctx.res;
            res.setStatus(200);
            res.setCharacterEncoding("UTF-8");
            res.setContentType("text/event-stream");
            res.addHeader(Header.CONNECTION, "close");
            res.addHeader(Header.CACHE_CONTROL, "no-cache");
            res.flushBuffer();

            ctx.req.startAsync(ctx.req, res);
            ctx.req.getAsyncContext().setTimeout(0);
            clientConsumer.accept(new SseClient(ctx));
            ctx.req.getAsyncContext().addListener(new AsyncListener() {
                @Override public void onComplete(AsyncEvent event) {}
                @Override public void onStartAsync(AsyncEvent event) {}
                @Override public void onTimeout(AsyncEvent event) {
                    event.getAsyncContext().complete();
                }
                @Override public void onError(AsyncEvent event) {
                    event.getAsyncContext().complete();
                }
            });
        }
    }
}
