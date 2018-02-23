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

// class/object/companion object
// fields/properties, both standalone and within a class/object
// functions/methods, both bound and unbound

val standAloneField = Handler { ctx -> }
fun standAloneMethod(context: Context) {}

class TestRouteOverview_Kotlin {

    @Test
    fun test_field_works() {
        assertThat(ObjectHandlers.lambdaField.metaInfo, `is`("io.javalin.routeoverview.ObjectHandlers.lambdaField"))
        assertThat(ClassHandlers().lambdaField.metaInfo, `is`("io.javalin.routeoverview.ClassHandlers.lambdaField"))
        assertThat(standAloneField.metaInfo, `is`("io.javalin.routeoverview.TestRouteOverview_KotlinKt.standAloneField"))
    }

    @Test
    fun test_class_works() {
        assertThat(ObjectHandlers.ImplementingClass().metaInfo, `is`("io.javalin.routeoverview.ObjectHandlers\$ImplementingClass.class"))
        assertThat(HandlerImplementation().metaInfo, `is`("io.javalin.routeoverview.HandlerImplementation.class"))
    }

    @Test
    fun methodRef_works() {
        assertThat(Util.getMetaInfo(ObjectHandlers::methodReference), `is`("io.javalin.routeoverview.ObjectHandlers::methodReference"))
        assertThat(Util.getMetaInfo(ClassHandlers()::methodReference), `is`("io.javalin.routeoverview.ClassHandlers::methodReference"))
        assertThat(Util.getMetaInfo(::standAloneMethod), `is`("io.javalin.routeoverview.TestRouteOverview_KotlinKt::standAloneMethod"))
    }

    @Test
    fun test_lambda_works() {
        assertThat(Util.getMetaInfo({}), `is`("io.javalin.routeoverview.TestRouteOverview_Kotlin::??? (anonymous lambda)"))
    }

}

object ObjectHandlers {
    val lambdaField = Handler { ctx -> }
    fun methodReference(context: Context) {}
    class ImplementingClass : Handler {
        override fun handle(context: Context) {}
    }
}

class ClassHandlers {
    val lambdaField = Handler { ctx -> }
    fun methodReference(context: Context) {}
}

class HandlerImplementation : Handler {
    override fun handle(ctx: Context?) {
    }
}
