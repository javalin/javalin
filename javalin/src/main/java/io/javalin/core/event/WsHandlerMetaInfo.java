package io.javalin.core.event;

import io.javalin.core.security.Role;
import io.javalin.websocket.WsHandlerType;

import java.util.Set;

public class WsHandlerMetaInfo {

    private final WsHandlerType handlerType;
    private final String path;
    private final Object handler;
    private final Set<Role> roles;

    public WsHandlerMetaInfo(WsHandlerType handlerType, String path, Object handler, Set<Role> roles) {
        this.handlerType = handlerType;
        this.path = path;
        this.handler = handler;
        this.roles = roles;
    }

    public WsHandlerType getHandlerType() {
        return handlerType;
    }

    public String getPath() {
        return path;
    }

    public Object getHandler() {
        return handler;
    }

    public Set<Role> getRoles() {
        return roles;
    }
}
