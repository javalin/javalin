/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core;

import io.javalin.Javalin;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class EventAttacher {

    private EventManager eventManager;

    public EventAttacher(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    // @formatter:off
    public Javalin serverStarting(@NotNull Runnable callback)    { return addLifecycleEvent(JavalinEvent.SERVER_STARTING,     callback); }
    public Javalin serverStarted(@NotNull Runnable callback)     { return addLifecycleEvent(JavalinEvent.SERVER_STARTED,      callback); }
    public Javalin serverStartFailed(@NotNull Runnable callback) { return addLifecycleEvent(JavalinEvent.SERVER_START_FAILED, callback); }
    public Javalin serverStopping(@NotNull Runnable callback)    { return addLifecycleEvent(JavalinEvent.SERVER_STOPPING,     callback); }
    public Javalin serverStopped(@NotNull Runnable callback)     { return addLifecycleEvent(JavalinEvent.SERVER_STOPPED,      callback); }
    // @formatter:on

    public Javalin handlerAdded(@NotNull Consumer<HandlerMetaInfo> callback) {
        this.eventManager.setHandlerAddedCallback(callback);
        return eventManager.getParentJavalin();
    }

    private Javalin addLifecycleEvent(@NotNull JavalinEvent event, @NotNull Runnable callback) {
        eventManager.getCallbackMap().get(event).add(callback);
        return eventManager.getParentJavalin();
    }

}
