/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.routeoverview;

import io.javalin.Handler;
import io.javalin.core.util.RouteOverviewUtil;

// this is sad.
public class RouteOverviewUtilWrapper {

    static String metaInfo(Handler handler) {
        return RouteOverviewUtil.INSTANCE.getMetaInfo(handler);
    }

    static String fieldName(Handler handler) {
        return RouteOverviewUtil.INSTANCE.getFieldName(handler);
    }

    static String methodName(Handler handler) {
        return RouteOverviewUtil.INSTANCE.getMethodName(handler);
    }

}
