/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.routeoverview

import io.javalin.Context
import io.javalin.Handler
import io.javalin.util.HandlerImplementation
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Ignore
import org.junit.Test

class TestRouteOverview_Kotlin {

    @Test
    fun test_field_works() {
        assertThat(RouteOverviewUtilWrapper.fieldName(KotlinHandlers.lambdaField), `is`("lambdaField"))
        assertThat(RouteOverviewUtilWrapper.metaInfo(KotlinHandlers.lambdaField), `is`("io.javalin.routeoverview.KotlinHandlers.lambdaField"))
    }

    @Test
    fun test_class_works() {
        assertThat(RouteOverviewUtilWrapper.metaInfo(KotlinHandlers.ImplementingClass()), `is`("io.javalin.routeoverview.KotlinHandlers\$ImplementingClass.class"))
        assertThat(RouteOverviewUtilWrapper.metaInfo(HandlerImplementation()), `is`("io.javalin.util.HandlerImplementation.class"))
    }

    @Test
    @Ignore("Broken")
    fun methodRef_works() {
        assertThat(RouteOverviewUtilWrapper.methodName(KotlinHandlers::methodReference), `is`("methodReference"))
        assertThat(RouteOverviewUtilWrapper.metaInfo(KotlinHandlers::methodReference), `is`("io.javalin.TestRouteOverview_Java::methodReference"))
    }

    @Test
    @Ignore("Broken")
    fun test_lambda_works() {
        assertThat(RouteOverviewUtilWrapper.metaInfo({ ctx -> ctx.result("") }), `is`("io.javalin.routeoverview.TestRouteOverview_Kotlin::??? (anonymous lambda)"))
    }

}

object KotlinHandlers {
    val lambdaField = Handler { ctx -> }
    fun methodReference(context: Context) {}
    class ImplementingClass : Handler {
        override fun handle(context: Context) {}
    }
}
