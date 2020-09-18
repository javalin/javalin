package io.javalin.core.event;

import io.javalin.core.security.Role;
import io.javalin.http.HandlerType;

import java.util.Set;

public class HandlerMetaInfo {

    private final HandlerType httpMethod;
    private final String path;
    private final Object handler;
    private final Set<Role> roles;

    public HandlerMetaInfo(HandlerType httpMethod, String path, Object handler, Set<Role> roles) {
        this.httpMethod = httpMethod;
        this.path = path;
        this.handler = handler;
        this.roles = roles;
    }

    public HandlerType getHttpMethod() {
        return httpMethod;
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
