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

    public void sendEvent(String event, String data) { //TODO: why is this
        this.emitter.event(event, data);
        ifClosedRunClosedHandler();
    }

    public void sendEvent(int id, String event, String data) { //TODO: not calling this?
        this.emitter.event(id, event, data);
        ifClosedRunClosedHandler();
    }

    public Context getContext() {
        return this.ctx;
    }

    private void ifClosedRunClosedHandler() {
        if (emitter.isClose()) {
            if (closeHandler != null) {
                closeHandler.accept( this );
            }
        }
    }

}
