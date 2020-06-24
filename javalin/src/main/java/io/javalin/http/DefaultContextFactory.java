/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class DefaultContextFactory implements ContextFactory {

    @Override
    public Context createContext(HttpServletRequest request, HttpServletResponse response, Map<Class<?>, Object> appAttributes) {
        return new Context(request, response, appAttributes);
    }
}
