// Javalin - https://javalin.io
// Copyright 2017 David Åse
// Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE

package javalin.core;

import java.util.HashMap;
import java.util.Map;

import javalin.ErrorHandler;
import javalin.Request;
import javalin.Response;

public class ErrorMapper {

    private Map<Integer, ErrorHandler> errorHandlerMap;

    public ErrorMapper() {
        this.errorHandlerMap = new HashMap<>();
    }

    public void put(Integer statusCode, ErrorHandler handler) {
        this.errorHandlerMap.put(statusCode, handler);
    }

    public void clear() {
        this.errorHandlerMap.clear();
    }

    void handle(int statusCode, Request request, Response response) {
        ErrorHandler errorHandler = errorHandlerMap.get(statusCode);
        if (errorHandler != null) {
            errorHandler.handle(request, response);
        }
    }
}
