package io.javalin.serversentevent;

import java.io.IOException;


public interface EventSourceEmitter {

    void emmit(String event, String data) throws IOException;
    void emmit(String id, String event, String data) throws IOException;

}