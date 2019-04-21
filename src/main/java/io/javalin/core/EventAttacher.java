/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core;

import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class EventAttacher {

    private EventManager eventManager;

    public EventAttacher(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    // @formatter:off
    public void serverStarting(@NotNull Runnable callback)    {  addLifecycleEvent(JavalinEvent.SERVER_STARTING,     callback); }
    public void serverStarted(@NotNull Runnable callback)     {  addLifecycleEvent(JavalinEvent.SERVER_STARTED,      callback); }
    public void serverStartFailed(@NotNull Runnable callback) {  addLifecycleEvent(JavalinEvent.SERVER_START_FAILED, callback); }
    public void serverStopping(@NotNull Runnable callback)    {  addLifecycleEvent(JavalinEvent.SERVER_STOPPING,     callback); }
    public void serverStopped(@NotNull Runnable callback)     {  addLifecycleEvent(JavalinEvent.SERVER_STOPPED,      callback); }
    // @formatter:on

    public void handlerAdded(@NotNull Consumer<HandlerMetaInfo> callback) {
        this.eventManager.getHandlerAddedHandlers().add(callback);
    }

    public void wsHandlerAdded(@NotNull Consumer<WsHandlerMetaInfo> callback) {
        eventManager.getWsHandlerAddedHandlers().add(callback);
    }

    private void addLifecycleEvent(@NotNull JavalinEvent event, @NotNull Runnable callback) {
        eventManager.getLifecycleHandlers().get(event).add(callback);
    }

}
