/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.event;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class EventManager {
    private final EnumMap<JavalinEvent, Set<EventHandler>> lifecycleHandlers = new EnumMap<>(JavalinEvent.class);

    private final Set<Consumer<HandlerMetaInfo>> handlerAddedHandlers = new LinkedHashSet<>();
    private final Set<Consumer<WsHandlerMetaInfo>> wsHandlerAddedHandlers = new LinkedHashSet<>();

    public EventManager() {
        for (JavalinEvent eventType : JavalinEvent.values()) {
            lifecycleHandlers.put(eventType, new HashSet<>());
        }
    }

    public void fireEvent(@NotNull JavalinEvent javalinEvent) {
        for (EventHandler eventHandler : lifecycleHandlers.get(javalinEvent)) {
            try {
                eventHandler.handleEvent();
            } catch (Exception e) {
                //TODO: Improve here - review if handleEvent() should throw?
                throw new RuntimeException(e);
            }
        }
    }

    public void fireHandlerAddedEvent(@NotNull HandlerMetaInfo metaInfo) {
        for (Consumer<HandlerMetaInfo> it : handlerAddedHandlers) {
            it.accept(metaInfo);
        }
    }
    public void fireWsHandlerAddedEvent(@NotNull WsHandlerMetaInfo metaInfo) {
        for (Consumer<WsHandlerMetaInfo> it : wsHandlerAddedHandlers) {
            it.accept(metaInfo);
        }
    }

    public EnumMap<JavalinEvent, Set<EventHandler>> getLifecycleHandlers() {
        return lifecycleHandlers;
    }

    public Set<Consumer<HandlerMetaInfo>> getHandlerAddedHandlers() {
        return handlerAddedHandlers;
    }

    public Set<Consumer<WsHandlerMetaInfo>> getWsHandlerAddedHandlers() {
        return wsHandlerAddedHandlers;
    }
}

