package io.javalin.routeoverview;

import io.javalin.Handler;;
import io.javalin.core.util.RouteOverviewUtil;

public class RouteOverviewUtilWrapper {

    static String getMetaInfo(Handler handler) {
        return RouteOverviewUtil.getMetaInfo(handler);
    }
}
