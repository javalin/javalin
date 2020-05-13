/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.event;

import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class EventListener {

    private EventManager eventManager;

    public EventListener(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    // @formatter:off
    public void serverStarting(@NotNull EventHandler eventHandler)    {  addLifecycleEvent(JavalinEvent.SERVER_STARTING,     eventHandler); }
    public void serverStarted(@NotNull EventHandler eventHandler)     {  addLifecycleEvent(JavalinEvent.SERVER_STARTED,      eventHandler); }
    public void serverStartFailed(@NotNull EventHandler eventHandler) {  addLifecycleEvent(JavalinEvent.SERVER_START_FAILED, eventHandler); }
    public void serverStopping(@NotNull EventHandler eventHandler)    {  addLifecycleEvent(JavalinEvent.SERVER_STOPPING,     eventHandler); }
    public void serverStopped(@NotNull EventHandler eventHandler)     {  addLifecycleEvent(JavalinEvent.SERVER_STOPPED,      eventHandler); }
    // @formatter:on

    public void handlerAdded(@NotNull Consumer<HandlerMetaInfo> callback) {
        this.eventManager.getHandlerAddedHandlers().add(callback);
    }

    public void wsHandlerAdded(@NotNull Consumer<WsHandlerMetaInfo> callback) {
        eventManager.getWsHandlerAddedHandlers().add(callback);
    }

    private void addLifecycleEvent(@NotNull JavalinEvent event, @NotNull EventHandler eventHandler) {
        eventManager.getLifecycleHandlers().get(event).add(eventHandler);
    }

}
