/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import io.javalin.embeddedserver.jetty.websocket.WebSocketConfig;
import io.javalin.security.Role;

public class ApiBuilder {

    @FunctionalInterface
    public interface EndpointGroup {
        void addEndpoints();
    }

    static void setStaticJavalin(@NotNull Javalin javalin) {
        staticJavalin = javalin;
    }

    static void clearStaticJavalin() {
        staticJavalin = null;
    }

    private static Javalin staticJavalin;
    private static Deque<String> pathDeque = new ArrayDeque<>();

    public static void path(@NotNull String path, @NotNull EndpointGroup endpointGroup) {
        path = path.startsWith("/") ? path : "/" + path;
        pathDeque.addLast(path);
        endpointGroup.addEndpoints();
        pathDeque.removeLast();
    }

    private static String prefixPath(@NotNull String path) {
        return pathDeque.stream().collect(Collectors.joining("")) + path;
    }

    private static Javalin staticInstance() {
        if (staticJavalin == null) {
            throw new IllegalStateException("The static API can only be called within a routes() call");
        }
        return staticJavalin;
    }

    // HTTP verbs
    public static void get(@NotNull String path, @NotNull Handler handler) {
        staticInstance().get(prefixPath(path), handler);
    }

    public static void post(@NotNull String path, @NotNull Handler handler) {
        staticInstance().post(prefixPath(path), handler);
    }

    public static void put(@NotNull String path, @NotNull Handler handler) {
        staticInstance().put(prefixPath(path), handler);
    }

    public static void patch(@NotNull String path, @NotNull Handler handler) {
        staticInstance().patch(prefixPath(path), handler);
    }

    public static void delete(@NotNull String path, @NotNull Handler handler) {
        staticInstance().delete(prefixPath(path), handler);
    }

    // Filters
    public static void before(@NotNull String path, @NotNull Handler handler) {
        staticInstance().before(prefixPath(path), handler);
    }

    public static void after(@NotNull String path, @NotNull Handler handler) {
        staticInstance().after(prefixPath(path), handler);
    }

    // Secured HTTP verbs
    public static void get(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().get(prefixPath(path), handler, permittedRoles);
    }

    public static void post(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().post(prefixPath(path), handler, permittedRoles);
    }

    public static void put(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().put(prefixPath(path), handler, permittedRoles);
    }

    public static void patch(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().patch(prefixPath(path), handler, permittedRoles);
    }

    public static void delete(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().delete(prefixPath(path), handler, permittedRoles);
    }

    // HTTP verbs (no path specified)
    public static void get(@NotNull Handler handler) {
        staticInstance().get(prefixPath(""), handler);
    }

    public static void post(@NotNull Handler handler) {
        staticInstance().post(prefixPath(""), handler);
    }

    public static void put(@NotNull Handler handler) {
        staticInstance().put(prefixPath(""), handler);
    }

    public static void patch(@NotNull Handler handler) {
        staticInstance().patch(prefixPath(""), handler);
    }

    public static void delete(@NotNull Handler handler) {
        staticInstance().delete(prefixPath(""), handler);
    }

    // Filters
    public static void before(@NotNull Handler handler) {
        staticInstance().before(prefixPath("/*"), handler);
    }

    public static void after(@NotNull Handler handler) {
        staticInstance().after(prefixPath("/*"), handler);
    }

    // Secured HTTP verbs (no path specified)
    public static void get(@NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().get(prefixPath(""), handler, permittedRoles);
    }

    public static void post(@NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().post(prefixPath(""), handler, permittedRoles);
    }

    public static void put(@NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().put(prefixPath(""), handler, permittedRoles);
    }

    public static void patch(@NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().patch(prefixPath(""), handler, permittedRoles);
    }

    public static void delete(@NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().delete(prefixPath(""), handler, permittedRoles);
    }

    public static void ws(@NotNull String path, @NotNull WebSocketConfig ws) {
        staticJavalin.ws(prefixPath(path), ws);
    }

    public static void ws(@NotNull String path, @NotNull Class webSocketClass) {
        staticJavalin.ws(prefixPath(path), webSocketClass);
    }

    public static void ws(@NotNull String path, @NotNull Object webSocketObject) {
        staticJavalin.ws(prefixPath(path), webSocketObject);
    }

}
