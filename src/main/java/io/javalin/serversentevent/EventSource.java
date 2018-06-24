package io.javalin.serversentevent;

public interface EventSource {

    void onOpen(SSEConnect connect);
    void onClose(SSEClose close);
    void sendEvent(String event, String data);
}