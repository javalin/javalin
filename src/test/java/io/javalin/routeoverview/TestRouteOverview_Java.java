/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.routeoverview;

import io.javalin.Context;
import io.javalin.Handler;
import io.javalin.core.util.RouteOverviewUtil;
import io.javalin.util.HandlerImplementation;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestRouteOverview_Java {

    private static Handler lambdaField = ctx -> {
    };

    private static class ImplementingClass implements Handler {
        @Override
        public void handle(Context context) {
        }
    }

    private void methodReference(Context context) {
    }

    @Test
    public void field_works() {
        assertThat(RouteOverviewUtil.getMetaInfo(lambdaField), is("io.javalin.routeoverview.TestRouteOverview_Java.lambdaField"));
    }

    @Test
    public void class_works() {
        assertThat(RouteOverviewUtil.getMetaInfo(new ImplementingClass()), is("io.javalin.routeoverview.TestRouteOverview_Java$ImplementingClass.class"));
        assertThat(RouteOverviewUtil.getMetaInfo(new HandlerImplementation()), is("io.javalin.util.HandlerImplementation.class"));
    }

    @Test
    public void methodRef_works() {
        assertThat(RouteOverviewUtil.getMetaInfo(new TestRouteOverview_Java()::methodReference), is("io.javalin.routeoverview.TestRouteOverview_Java::methodReference"));
    }

    @Test
    public void lambda_works() {
        assertThat(RouteOverviewUtil.getMetaInfo(ctx -> ctx.result("")), is("io.javalin.routeoverview.TestRouteOverview_Java::??? (anonymous lambda)"));
    }

}
