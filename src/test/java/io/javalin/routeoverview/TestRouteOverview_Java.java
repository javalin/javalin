/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.routeoverview;

import io.javalin.core.util.RouteOverviewUtil;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.junit.Ignore;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestRouteOverview_Java {

    private static Handler lambdaField = ctx -> {
    };

    private void methodReference(Context context) {
    }

    @Test
    public void field_works() {
        assertThat(RouteOverviewUtil.getMetaInfo(lambdaField), is("io.javalin.routeoverview.TestRouteOverview_Java.lambdaField"));
    }

    @Test
    public void class_works() {
        assertThat(RouteOverviewUtil.getMetaInfo(new InnerHandlerImplementation()), is("io.javalin.routeoverview.TestRouteOverview_Java$InnerHandlerImplementation.class"));
        assertThat(RouteOverviewUtil.getMetaInfo(new HandlerImplementation()), is("io.javalin.routeoverview.HandlerImplementation.class"));
    }

    @Test
    @Ignore("Currently disabled because it's broken in jdk9+")
    public void method_reference_works() {
        assertThat(RouteOverviewUtil.getMetaInfo((Handler) new TestRouteOverview_Java()::methodReference), is("io.javalin.routeoverview.TestRouteOverview_Java::methodReference"));
    }

    @Test
    public void lambda_works() {
        assertThat(RouteOverviewUtil.getMetaInfo((Handler) (ctx -> ctx.result(""))), is("io.javalin.routeoverview.TestRouteOverview_Java::??? (anonymous lambda)"));
    }

    private static class InnerHandlerImplementation implements Handler {
        @Override
        public void handle(Context context) {
        }
    }

}
