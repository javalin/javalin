package io.javalin.serversentevent;

public interface Emitter {
    void event(String event, String data);
    boolean isClose();
}
