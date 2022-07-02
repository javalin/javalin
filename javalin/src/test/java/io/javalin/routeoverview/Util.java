/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.routeoverview;

import io.javalin.http.Handler;
import io.javalin.plugin.RouteOverviewUtil;

public class Util {
    static String getMetaInfo(Handler handler) {
        return RouteOverviewUtil.getMetaInfo(handler);
    }
}
