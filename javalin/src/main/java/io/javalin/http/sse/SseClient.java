package io.javalin.http.sse;

import io.javalin.http.Context;

public class SseClient {

    public final Context ctx;
    private Emitter emitter;
    private Runnable closeCallback = null;

    public SseClient(Context ctx) {
        this.emitter = new Emitter(ctx.req.getAsyncContext());
        this.ctx = ctx;
    }

    public void onClose(Runnable closeCallback) {
        this.closeCallback = closeCallback;
    }

    public void sendEvent(String data) {
        sendEvent("message", data);
    }

    public void sendEvent(String event, String data) {
        sendEvent(event, data, null);
    }

    public void sendEvent(String event, String data, String id) {
        this.emitter.emit(event, data, id);
        if (emitter.isClose() && closeCallback != null) {
            closeCallback.run();
        }
    }

}
