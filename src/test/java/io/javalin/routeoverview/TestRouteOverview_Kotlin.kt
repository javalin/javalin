/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.routeoverview

import io.javalin.Context
import io.javalin.Handler
import io.javalin.core.util.metaInfo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class TestRouteOverview_Kotlin {

    @Test
    fun test_field_works() {
        assertThat(KotlinHandlers.lambdaField.metaInfo, `is`("io.javalin.routeoverview.KotlinHandlers.lambdaField"))
    }

    @Test
    fun test_class_works() {
        assertThat(KotlinHandlers.ImplementingClass().metaInfo, `is`("io.javalin.routeoverview.KotlinHandlers\$ImplementingClass.class"))
        assertThat(HandlerImplementation().metaInfo, `is`("io.javalin.routeoverview.HandlerImplementation.class"))
    }

    @Test
    fun methodRef_works() {
        assertThat(RouteOverviewUtilWrapper.getMetaInfo(KotlinHandlers::methodReference), `is`("io.javalin.routeoverview.KotlinHandlers::methodReference"))
    }

    @Test
    fun test_lambda_works() {
        assertThat(RouteOverviewUtilWrapper.getMetaInfo({}), `is`("io.javalin.routeoverview.TestRouteOverview_Kotlin::??? (anonymous lambda)"))
    }

}

object KotlinHandlers {
    val lambdaField = Handler { ctx -> }
    fun methodReference(context: Context) {}
    class ImplementingClass : Handler {
        override fun handle(context: Context) {}
    }
}

class HandlerImplementation : Handler {
    override fun handle(ctx: Context?) {
    }
}