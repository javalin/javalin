/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toConcurrentMap;

/**
 * The possible Handler types one can use in Javalin.
 * This includes all standard HTTP methods (e.g.: GET, POST, …),
 * as well as Javalin specific operations.
 */
public class HandlerType {
    private final String name;
    private final boolean isHttpMethod;

    public HandlerType(String name, boolean isHttpMethod) {
        this.name = name;
        this.isHttpMethod = isHttpMethod;
    }

    public HandlerType(String name) {
        this(name, true);
    }

    public String getName() {
        return name;
    }

    public boolean isHttpMethod() {
        return isHttpMethod;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        HandlerType that = (HandlerType) obj;
        return isHttpMethod == that.isHttpMethod && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, isHttpMethod);
    }

    /**
     * End of instance methods. Start of static methods and constants.
     */

    // Standard HTTP methods
    public static final HandlerType GET = new HandlerType("GET");
    public static final HandlerType POST = new HandlerType("POST");
    public static final HandlerType PUT = new HandlerType("PUT");
    public static final HandlerType PATCH = new HandlerType("PATCH");
    public static final HandlerType DELETE = new HandlerType("DELETE");
    public static final HandlerType HEAD = new HandlerType("HEAD");
    public static final HandlerType TRACE = new HandlerType("TRACE");
    public static final HandlerType CONNECT = new HandlerType("CONNECT");
    public static final HandlerType OPTIONS = new HandlerType("OPTIONS");

    // Javalin-specific handlers
    public static final HandlerType BEFORE = new HandlerType("BEFORE", false);
    public static final HandlerType BEFORE_MATCHED = new HandlerType("BEFORE_MATCHED", false);
    public static final HandlerType AFTER_MATCHED = new HandlerType("AFTER_MATCHED", false);
    public static final HandlerType WEBSOCKET_BEFORE_UPGRADE = new HandlerType("WEBSOCKET_BEFORE_UPGRADE", false);
    public static final HandlerType WEBSOCKET_AFTER_UPGRADE = new HandlerType("WEBSOCKET_AFTER_UPGRADE", false);
    public static final HandlerType AFTER = new HandlerType("AFTER", false);

    // Standard methods list and lookup
    private static final List<HandlerType> DEFAULT_METHODS = List.of(
        GET, POST, PUT, PATCH, DELETE, HEAD, TRACE, CONNECT, OPTIONS,
        BEFORE, BEFORE_MATCHED, AFTER_MATCHED, WEBSOCKET_BEFORE_UPGRADE, WEBSOCKET_AFTER_UPGRADE, AFTER
    );

    private static final List<HandlerType> COMMON_HTTP_METHODS = List.of(GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE);

    private static final Map<String, HandlerType> METHOD_MAP = DEFAULT_METHODS.stream()
        .collect(toConcurrentMap(HandlerType::getName, method -> method)); // start with default methods

    /**
     * Find a HandlerType by name. Returns existing constants for standard methods,
     * or creates a new HandlerType for custom methods.
     */
    public static HandlerType findOrCreate(String name) {
        if (!name.matches("^[A-Z]+$")) { // only accept uppercase letters
            throw new IllegalArgumentException("Invalid HTTP method name: " + name);
        }
        String upperName = name.toUpperCase();
        return METHOD_MAP.computeIfAbsent(upperName, key -> new HandlerType(key, true));
    }

    /**
     * Returns all standard HandlerType values (equivalent to enum values()).
     * Does not include dynamically created custom methods.
     */
    public static List<HandlerType> values() {
        return DEFAULT_METHODS;
    }

    /**
     * Returns only the common HTTP methods (GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE).
     * Excludes Javalin-specific handlers and CONNECT method.
     */
    public static List<HandlerType> commonHttp() {
        return COMMON_HTTP_METHODS;
    }
}
