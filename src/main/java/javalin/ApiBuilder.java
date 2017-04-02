// Javalin - https://javalin.io
// Copyright 2017 David Åse
// Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE

package javalin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import javalin.security.Role;

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

    // Everything below here is copied from the end of Javalin.java

    // HTTP verbs
    public static void get(String path, Handler handler) {
        staticJavalin.get(prefixPath(path), handler);
    }

    public static void post(String path, Handler handler) {
        staticJavalin.post(prefixPath(path), handler);
    }

    public static void put(String path, Handler handler) {
        staticJavalin.put(prefixPath(path), handler);
    }

    public static void patch(String path, Handler handler) {
        staticJavalin.patch(prefixPath(path), handler);
    }

    public static void delete(String path, Handler handler) {
        staticJavalin.delete(prefixPath(path), handler);
    }

    public static void head(String path, Handler handler) {
        staticJavalin.head(prefixPath(path), handler);
    }

    public static void trace(String path, Handler handler) {
        staticJavalin.trace(prefixPath(path), handler);
    }

    public static void connect(String path, Handler handler) {
        staticJavalin.connect(prefixPath(path), handler);
    }

    public static void options(String path, Handler handler) {
        staticJavalin.options(prefixPath(path), handler);
    }

    // Secured HTTP verbs
    public static void get(String path, Handler handler, List<Role> permittedRoles) {
        staticJavalin.get(prefixPath(path), handler, permittedRoles);
    }

    public static void post(String path, Handler handler, List<Role> permittedRoles) {
        staticJavalin.post(prefixPath(path), handler, permittedRoles);
    }

    public static void put(String path, Handler handler, List<Role> permittedRoles) {
        staticJavalin.put(prefixPath(path), handler, permittedRoles);
    }

    public static void patch(String path, Handler handler, List<Role> permittedRoles) {
        staticJavalin.patch(prefixPath(path), handler, permittedRoles);
    }

    public static void delete(String path, Handler handler, List<Role> permittedRoles) {
        staticJavalin.delete(prefixPath(path), handler, permittedRoles);
    }

    public static void head(String path, Handler handler, List<Role> permittedRoles) {
        staticJavalin.head(prefixPath(path), handler, permittedRoles);
    }

    public static void trace(String path, Handler handler, List<Role> permittedRoles) {
        staticJavalin.trace(prefixPath(path), handler, permittedRoles);
    }

    public static void connect(String path, Handler handler, List<Role> permittedRoles) {
        staticJavalin.connect(prefixPath(path), handler, permittedRoles);
    }

    public static void options(String path, Handler handler, List<Role> permittedRoles) {
        staticJavalin.options(prefixPath(path), handler, permittedRoles);
    }

    // Filters
    public static void before(String path, Handler handler) {
        staticJavalin.before(prefixPath(path), handler);
    }

    public static void before(Handler handler) {
        staticJavalin.before(prefixPath("/*"), handler);
    }

    public static void after(String path, Handler handler) {
        staticJavalin.after(prefixPath(path), handler);
    }

    public static void after(Handler handler) {
        staticJavalin.after(prefixPath("/*"), handler);
    }

}
