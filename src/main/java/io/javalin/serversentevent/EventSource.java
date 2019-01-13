package io.javalin.serversentevent;

import io.javalin.Context;
import java.util.function.Consumer;

public class EventSource {

    private Emitter emitter;
    private Context ctx;
    private Consumer<EventSource> closeHandler = null;

    public EventSource(Context ctx) {
        this.emitter = new Emitter(ctx.req.getAsyncContext());
        this.ctx = ctx;
    }

    public void onClose(Consumer<EventSource> close) {
        this.closeHandler = close;
    }

    public void sendEvent(String event, String data) {
        sendEvent(event, data, null);
    }

    public void sendEvent(String event, String data, String id) {
        this.emitter.event(event, data, id);
        if (emitter.isClose() && closeHandler != null) {
            closeHandler.accept(this);
        }
    }

    public Context getContext() {
        return this.ctx;
    }

}
