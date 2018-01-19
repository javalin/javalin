/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.routeoverview;

import io.javalin.Context;
import io.javalin.Handler;
import io.javalin.Javalin;
import io.javalin.core.util.RouteOverviewUtil;
import io.javalin.util.HandlerImplementation;
import static io.javalin.TestAccessManager.MyRoles.ROLE_ONE;
import static io.javalin.TestAccessManager.MyRoles.ROLE_THREE;
import static io.javalin.TestAccessManager.MyRoles.ROLE_TWO;
import static io.javalin.security.Role.roles;

public class TestRouteOverview_visual {

    public static void main(String[] args) {
        Javalin app = Javalin.start(7000);
        app.get("/1", TestRouteOverview_visual.lambdaField);
        app.get("/2", new ImplementingClass());
        app.get("/3", new HandlerImplementation());
        app.get("/4", TestRouteOverview_visual::methodReference);
        app.get("/5", ctx -> ctx.result(""));
        app.get("/6", ctx -> ctx.result(""), roles(ROLE_ONE));
        app.get("/7", TestRouteOverview_visual.lambdaField, roles(ROLE_ONE, ROLE_THREE));
        app.get("/8", TestRouteOverview_visual::methodReference, roles(ROLE_ONE, ROLE_TWO));
        app.get("/", ctx -> ctx.html(RouteOverviewUtil.INSTANCE.createHtmlOverview(app)));
    }

    private static Handler lambdaField = ctx -> {
    };

    private static class ImplementingClass implements Handler {
        @Override
        public void handle(Context context) {
        }
    }

    private static void methodReference(Context context) {
    }

}
