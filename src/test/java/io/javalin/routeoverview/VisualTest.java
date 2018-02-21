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

public class VisualTest {

    public static void main(String[] args) {
        Javalin app = Javalin.start(7000);
        app.get("/1", VisualTest.lambdaField);
        app.get("/2", new ImplementingClass());
        app.get("/3", new HandlerImplementation());
        app.get("/4", VisualTest::methodReference);
        app.get("/5", ctx -> ctx.result(""));
        app.get("/6", ctx -> ctx.result(""), roles(ROLE_ONE));
        app.get("/7", VisualTest.lambdaField, roles(ROLE_ONE, ROLE_THREE));
        app.get("/8", VisualTest::methodReference, roles(ROLE_ONE, ROLE_TWO));
        app.get("/", ctx -> ctx.html(RouteOverviewUtil.createHtmlOverview(app)));
        app.head("/head", ctx -> ctx.result(""));
        app.get("/get", ctx -> ctx.result(""));
        app.post("/post", ctx -> ctx.result(""));
        app.put("/put", ctx -> ctx.result(""));
        app.patch("/patch", ctx -> ctx.result(""));
        app.delete("/delete", ctx -> ctx.result(""));
        app.options("/options", ctx -> ctx.result(""));
        app.trace("/trace", ctx -> ctx.result(""));
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
