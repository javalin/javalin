package io.javalin.serversentevent;

public class EventSourceImpl implements EventSource {

    private Emitter emitter;
    private SSEClose close;

    public EventSourceImpl(Emitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void onOpen(SSEConnect open) {
        open.handler( this );
    }

    @Override
    public void onClose(SSEClose close) {
        this.close = close;
    }

    @Override
    public void sendEvent(String event, String data) {
        emitter.event( event, data );
        if(emitter.isClose()) {
            close.handler( this );
        }
    }
}
