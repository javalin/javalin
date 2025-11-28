/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.routeoverview;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.bundled.RouteOverviewUtil;
import io.javalin.routeoverview.VisualTest.HandlerImplementation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestRouteOverviewInJava {

    private static final Handler lambdaField = ctx -> {};

    @Test
    public void field_works() {
        assertThat(RouteOverviewUtil.getMetaInfo(lambdaField)).isEqualTo("io.javalin.routeoverview.TestRouteOverviewInJava.lambdaField");
    }

    @Test
    public void class_works() {
        assertThat(RouteOverviewUtil.getMetaInfo(new InnerHandlerImplementation())).isEqualTo("io.javalin.routeoverview.TestRouteOverviewInJava$InnerHandlerImplementation.class");
        assertThat(RouteOverviewUtil.getMetaInfo(new HandlerImplementation())).isEqualTo("io.javalin.routeoverview.VisualTest$HandlerImplementation.class");
    }

    @Test
    public void lambda_works() {
        assertThat(RouteOverviewUtil.getMetaInfo((Handler) (ctx -> ctx.result("")))).isEqualTo("io.javalin.routeoverview.TestRouteOverviewInJava::??? (anonymous lambda)");
    }

    private static class InnerHandlerImplementation implements Handler {
        @Override
        public void handle(@NotNull Context context) {}
    }

}
