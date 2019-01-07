package io.javalin.serversentevent;

import io.javalin.Context;

import java.util.Optional;

public class EventSource {
    private final Emitter emitter;
    private final Context context;
    private Optional<SseClose> close = Optional.empty();

    public EventSource(Emitter emitter, Context context ) {
        this.emitter = emitter;
        this.context = context;
    }

    public void onClose(SseClose close) {
        this.close = Optional.of( close );
    }

    public void sendEvent(String event, String data) {
        this.emitter.event(event, data);

        if(emitter.isClose()) {
            close.ifPresent( closeHandle -> closeHandle.handle( this ) );
        }
    }

    public Context getContext() {
        return this.context;
    }

    public void sendEvent(int id, String event, String data) {
        this.emitter.event(id, event, data);

        if(emitter.isClose()) {
            close.ifPresent( closeHandle -> closeHandle.handle( this ) );
        }
    }
}
