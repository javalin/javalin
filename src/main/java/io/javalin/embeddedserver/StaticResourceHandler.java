/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.embeddedserver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface StaticResourceHandler {
    // should return if request has been handled
    boolean handle(HttpServletRequest httpRequest, HttpServletResponse httpResponse);
}
