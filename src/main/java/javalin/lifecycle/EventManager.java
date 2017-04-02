// Javalin - https://javalin.io
// Copyright 2017 David Åse
// Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE

package javalin.lifecycle;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javalin.Javalin;

public class EventManager {

    private Map<Event.Type, List<EventListener>> listenerMap;

    public EventManager() {
        this.listenerMap = Stream.of(Event.Type.values()).collect(Collectors.toMap(v -> v, v -> new LinkedList<>()));
    }

    public synchronized void addEventListener(Event.Type type, EventListener listener) {
        listenerMap.get(type).add(listener);
    }

    public void fireEvent(Event.Type type, Javalin javalin) {
        listenerMap.get(type).forEach(listener -> listener.handleEvent(new Event(type, javalin)));
    }

    public void fireEvent(Event.Type type) {
        fireEvent(type, null);
    }

}
