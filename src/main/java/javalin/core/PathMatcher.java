// Javalin - https://javalin.io
// Copyright 2017 David Åse
// Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE

package javalin.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javalin.Handler;
import javalin.core.util.Util;

public class PathMatcher {

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

    public HandlerMatch findEndpointHandler(Handler.Type type, String path) {
        List<HandlerEntry> handlerEntries = findTargetsForRequestedHandler(type, path);
        if (handlerEntries.size() == 0) {
            return null;
        }
        HandlerEntry entry = handlerEntries.get(0);
        return new HandlerMatch(entry.handler, entry.path, path);
    }

    public List<HandlerMatch> findFilterHandlers(Handler.Type type, String path) {
        return findTargetsForRequestedHandler(type, path).stream()
            .map(handlerEntry -> new HandlerMatch(handlerEntry.handler, handlerEntry.path, path))
            .collect(Collectors.toList());
    }

    private List<HandlerEntry> findTargetsForRequestedHandler(Handler.Type type, String path) {
        return handlerEntries.stream()
            .filter(r -> match(r, type, path))
            .collect(Collectors.toList());
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
