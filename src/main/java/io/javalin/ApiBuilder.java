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

import io.javalin.security.Role;

public class ApiBuilder {

    @FunctionalInterface
    public interface EndpointGroup {
        void addEndpoints();
    }

    static void setStaticJavalin(Javalin javalin) {
        staticJavalin = javalin;
    }

    static void clearStaticJavalin() {
        staticJavalin = null;
    }

    private static Javalin staticJavalin;
    private static Deque<String> pathDeque = new ArrayDeque<>();

    public static void path(String path, EndpointGroup endpointGroup) {
        path = path.startsWith("/") ? path : "/" + path;
        pathDeque.addLast(path);
        endpointGroup.addEndpoints();
        pathDeque.removeLast();
    }

    private static String prefixPath(String path) {
        return pathDeque.stream().collect(Collectors.joining("")) + path;
    }

    private static Javalin staticInstance() {
        if (staticJavalin == null) {
            throw new IllegalStateException("The static API can only be called within a routes() call");
        }
        return staticJavalin;
    }

    // HTTP verbs
    public static void get(String path, Handler handler) {
        staticInstance().get(prefixPath(path), handler);
    }

    public static void post(String path, Handler handler) {
        staticInstance().post(prefixPath(path), handler);
    }

    public static void put(String path, Handler handler) {
        staticInstance().put(prefixPath(path), handler);
    }

    public static void patch(String path, Handler handler) {
        staticInstance().patch(prefixPath(path), handler);
    }

    public static void delete(String path, Handler handler) {
        staticInstance().delete(prefixPath(path), handler);
    }

    // Filters
    public static void before(String path, Handler handler) {
        staticInstance().before(prefixPath(path), handler);
    }

    public static void after(String path, Handler handler) {
        staticInstance().after(prefixPath(path), handler);
    }

    // Secured HTTP verbs
    public static void get(String path, Handler handler, List<Role> permittedRoles) {
        staticInstance().get(prefixPath(path), handler, permittedRoles);
    }

    public static void post(String path, Handler handler, List<Role> permittedRoles) {
        staticInstance().post(prefixPath(path), handler, permittedRoles);
    }

    public static void put(String path, Handler handler, List<Role> permittedRoles) {
        staticInstance().put(prefixPath(path), handler, permittedRoles);
    }

    public static void patch(String path, Handler handler, List<Role> permittedRoles) {
        staticInstance().patch(prefixPath(path), handler, permittedRoles);
    }

    public static void delete(String path, Handler handler, List<Role> permittedRoles) {
        staticInstance().delete(prefixPath(path), handler, permittedRoles);
    }

    // HTTP verbs (no path specified)
    public static void get(Handler handler) {
        staticInstance().get(prefixPath(""), handler);
    }

    public static void post(Handler handler) {
        staticInstance().post(prefixPath(""), handler);
    }

    public static void put(Handler handler) {
        staticInstance().put(prefixPath(""), handler);
    }

    public static void patch(Handler handler) {
        staticInstance().patch(prefixPath(""), handler);
    }

    public static void delete(Handler handler) {
        staticInstance().delete(prefixPath(""), handler);
    }

    // Filters
    public static void before(Handler handler) {
        staticInstance().before(prefixPath("/*"), handler);
    }

    public static void after(Handler handler) {
        staticInstance().after(prefixPath("/*"), handler);
    }

    // Secured HTTP verbs (no path specified)
    public static void get(Handler handler, List<Role> permittedRoles) {
        staticInstance().get(prefixPath(""), handler, permittedRoles);
    }

    public static void post(Handler handler, List<Role> permittedRoles) {
        staticInstance().post(prefixPath(""), handler, permittedRoles);
    }

    public static void put(Handler handler, List<Role> permittedRoles) {
        staticInstance().put(prefixPath(""), handler, permittedRoles);
    }

    public static void patch(Handler handler, List<Role> permittedRoles) {
        staticInstance().patch(prefixPath(""), handler, permittedRoles);
    }

    public static void delete(Handler handler, List<Role> permittedRoles) {
        staticInstance().delete(prefixPath(""), handler, permittedRoles);
    }

}
