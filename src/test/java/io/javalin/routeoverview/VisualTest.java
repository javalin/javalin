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
        Javalin app = Javalin.create().enableCorsForAllOrigins().start();
        app.get("/", ctx -> ctx.redirect("/route-overview"));
        app.get("/route-overview", ctx -> ctx.html(RouteOverviewUtil.createHtmlOverview(app)));
        app.get("/just-some-path", new HandlerImplementation());
        app.post("/test/:hmm/", VisualTest::methodReference);
        app.put("/user/*", ctx -> ctx.result(""), roles(ROLE_ONE));
        app.get("/nonsense-paths/:test", VisualTest.lambdaField, roles(ROLE_ONE, ROLE_THREE));
        app.delete("/just-words", VisualTest::methodReference, roles(ROLE_ONE, ROLE_TWO));
        app.before("*", VisualTest.lambdaField);
        app.after("*", VisualTest.lambdaField);
        app.head("/check/the/head", VisualTest::methodReference);
        app.get("/:path1/:path2", VisualTest.lambdaField);
        app.post("/user/create",  VisualTest::methodReference, roles(ROLE_ONE, ROLE_TWO));
        app.put("/user/:user-id", VisualTest.lambdaField);
        app.patch("/patchy-mcpatchface", new ImplementingClass(), roles(ROLE_ONE, ROLE_TWO));
        app.delete("/users/:user-id", new HandlerImplementation());
        app.options("/what/:are/*/my-options", new HandlerImplementation());
        app.trace("/tracer", new HandlerImplementation());
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
