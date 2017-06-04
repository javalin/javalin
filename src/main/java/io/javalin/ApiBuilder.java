/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
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

    // Everything below here is copied from the end of Javalin.java

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

    public static void head(String path, Handler handler) {
        staticInstance().head(prefixPath(path), handler);
    }

    public static void trace(String path, Handler handler) {
        staticInstance().trace(prefixPath(path), handler);
    }

    public static void connect(String path, Handler handler) {
        staticInstance().connect(prefixPath(path), handler);
    }

    public static void options(String path, Handler handler) {
        staticInstance().options(prefixPath(path), handler);
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

    public static void head(String path, Handler handler, List<Role> permittedRoles) {
        staticInstance().head(prefixPath(path), handler, permittedRoles);
    }

    public static void trace(String path, Handler handler, List<Role> permittedRoles) {
        staticInstance().trace(prefixPath(path), handler, permittedRoles);
    }

    public static void connect(String path, Handler handler, List<Role> permittedRoles) {
        staticInstance().connect(prefixPath(path), handler, permittedRoles);
    }

    public static void options(String path, Handler handler, List<Role> permittedRoles) {
        staticInstance().options(prefixPath(path), handler, permittedRoles);
    }

    // Filters
    public static void before(String path, Handler handler) {
        staticInstance().before(prefixPath(path), handler);
    }

    public static void before(Handler handler) {
        staticInstance().before(prefixPath("/*"), handler);
    }

    public static void after(String path, Handler handler) {
        staticInstance().after(prefixPath(path), handler);
    }

    public static void after(Handler handler) {
        staticInstance().after(prefixPath("/*"), handler);
    }

}
