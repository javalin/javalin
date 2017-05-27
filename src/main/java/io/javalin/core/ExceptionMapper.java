/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.core;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.ExceptionHandler;
import io.javalin.Request;
import io.javalin.HaltException;
import io.javalin.Response;

public class ExceptionMapper {

    private static Logger log = LoggerFactory.getLogger(ExceptionMapper.class);

    private Map<Class<? extends Exception>, ExceptionHandler> exceptionMap;

    public ExceptionMapper() {
        this.exceptionMap = new HashMap<>();
    }

    public void put(Class<? extends Exception> exceptionClass, ExceptionHandler handler) {
        this.exceptionMap.put(exceptionClass, handler);
    }

    public void clear() {
        this.exceptionMap.clear();
    }


    void handle(Exception e, Request request, Response response) {
        if (e instanceof HaltException) {
            response.status(((HaltException) e).statusCode);
            response.body(((HaltException) e).body);
            return;
        }
        ExceptionHandler exceptionHandler = this.getHandler(e.getClass());
        if (exceptionHandler != null) {
            exceptionHandler.handle(e, request, response);
        } else {
            log.error("Uncaught exception", e);
            response.body("Internal server error");
            response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }


    public ExceptionHandler getHandler(Class<? extends Exception> exceptionClass) {
        if (this.exceptionMap.containsKey(exceptionClass)) {
            return this.exceptionMap.get(exceptionClass);
        }
        Class<?> superclass = exceptionClass.getSuperclass();
        while (superclass != null) {
            if (this.exceptionMap.containsKey(superclass)) {
                ExceptionHandler matchingHandler = this.exceptionMap.get(superclass);
                this.exceptionMap.put(exceptionClass, matchingHandler); // superclass was found, avoid search next time
                return matchingHandler;
            }
            superclass = superclass.getSuperclass();
        }
        this.exceptionMap.put(exceptionClass, null); // nothing was found, avoid search next time
        return null;
    }
}
