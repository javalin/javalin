/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface Handler {

    enum Type {
        GET, POST, PUT, PATCH, DELETE, HEAD, TRACE, CONNECT, OPTIONS, BEFORE, AFTER, INVALID;

        private static Map<String, Type> methodMap = Stream.of(Type.values()).collect(Collectors.toMap(Object::toString, v -> v));

        public static Type fromServletRequest(HttpServletRequest httpRequest) {
            String key = Optional.ofNullable(httpRequest.getHeader("X-HTTP-Method-Override")).orElse(httpRequest.getMethod());
            return methodMap.getOrDefault(key.toUpperCase(), INVALID);
        }
    }

    void handle(Request request, Response response) throws Exception;
}