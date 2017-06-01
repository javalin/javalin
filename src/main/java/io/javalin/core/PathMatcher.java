/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.Handler;
import io.javalin.core.util.Util;

public class PathMatcher {

    private static Logger log = LoggerFactory.getLogger(PathMatcher.class);

    private List<HandlerEntry> handlerEntries;

    public PathMatcher() {
        handlerEntries = new ArrayList<>();
    }

    public void add(Handler.Type type, String path, Handler handler) {
        handlerEntries.add(new HandlerEntry(type, path, handler));
    }

    public void clear() {
        handlerEntries.clear();
    }

    public List<HandlerMatch> findHandlers(Handler.Type type, String path) {
        return findTargetsForRequestedHandler(type, path).stream()
            .map(handlerEntry -> new HandlerMatch(handlerEntry.handler, handlerEntry.path, path))
            .collect(Collectors.toList());
    }

    private List<HandlerEntry> findTargetsForRequestedHandler(Handler.Type type, String path) {
        return handlerEntries.stream()
            .filter(he -> match(he, type, path))
            .collect(Collectors.toList());
    }

    public String findHandlerPath(Predicate<HandlerEntry> predicate) {
        List<HandlerEntry> entries = handlerEntries.stream()
            .filter(predicate)
            .collect(Collectors.toList());
        if (entries.size() > 1) {
            log.warn("More than one path found for handler, returning first match: '{} {}'", entries.get(0).type, entries.get(0).path);
        }
        return entries.size() > 0 ? entries.get(0).path : null;
    }

    // TODO: Consider optimizing this
    private static boolean match(HandlerEntry handlerEntry, Handler.Type requestType, String requestPath) {
        if (handlerEntry.type != requestType) {
            return false;
        }
        if (endingSlashesDoNotMatch(handlerEntry.path, requestPath)) {
            return false;
        }
        if (handlerEntry.path.equals(requestPath)) { // identical paths
            return true;
        }
        return matchParamAndWildcard(handlerEntry.path, requestPath);
    }

    private static boolean matchParamAndWildcard(String handlerPath, String requestPath) {

        List<String> handlerPaths = Util.pathToList(handlerPath);
        List<String> requestPaths = Util.pathToList(requestPath);

        int numHandlerPaths = handlerPaths.size();
        int numRequestPaths = requestPaths.size();

        if (numHandlerPaths == numRequestPaths) {
            for (int i = 0; i < numHandlerPaths; i++) {
                String handlerPathPart = handlerPaths.get(i);
                String requestPathPart = requestPaths.get(i);
                if (handlerPathPart.equals("*") && handlerPath.endsWith("*") && (i == numHandlerPaths - 1)) {
                    return true;
                }
                if (!handlerPathPart.equals("*") && !handlerPathPart.startsWith(":") && !handlerPathPart.equals(requestPathPart)) {
                    return false;
                }
            }
            return true;
        }
        if (handlerPath.endsWith("*") && numHandlerPaths < numRequestPaths) {
            for (int i = 0; i < numHandlerPaths; i++) {
                String handlerPathPart = handlerPaths.get(i);
                String requestPathPart = requestPaths.get(i);
                if (handlerPathPart.equals("*") && handlerPath.endsWith("*") && (i == numHandlerPaths - 1)) {
                    return true;
                }
                if (!handlerPathPart.startsWith(":") && !handlerPathPart.equals("*") && !handlerPathPart.equals(requestPathPart)) {
                    return false;
                }
            }
            return false;
        }
        return false;
    }

    private static boolean endingSlashesDoNotMatch(String handlerPath, String requestPath) {
        return requestPath.endsWith("/") && !handlerPath.endsWith("/")
            || !requestPath.endsWith("/") && handlerPath.endsWith("/");
    }

}
