/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.routeoverview

import io.javalin.http.Context
import io.javalin.http.Handler
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

// class/object/companion object
// fields/properties, both standalone and within a class/object
// functions/methods, both bound and unbound

val standAloneField = Handler {}
fun standAloneMethod(ctx: Context) {}

class TestRouteOverview_Kotlin {

    @Test
    fun `field works`() {
        assertThat(Util.getMetaInfo(ObjectHandlers.lambdaField), `is`("io.javalin.routeoverview.ObjectHandlers.lambdaField"))
        assertThat(Util.getMetaInfo(ClassHandlers().lambdaField), `is`("io.javalin.routeoverview.ClassHandlers.lambdaField"))
        assertThat(Util.getMetaInfo(standAloneField), `is`("io.javalin.routeoverview.TestRouteOverview_KotlinKt.standAloneField"))
    }

    @Test
    fun `class works`() {
        assertThat(Util.getMetaInfo(ObjectHandlers.ImplementingClass()), `is`("io.javalin.routeoverview.ObjectHandlers\$ImplementingClass.class"))
        assertThat(Util.getMetaInfo(HandlerImplementation()), `is`("io.javalin.routeoverview.HandlerImplementation.class"))
    }

    @Test
    fun `method reference works`() {
        assertThat(Util.getMetaInfo(ObjectHandlers::methodReference), `is`("io.javalin.routeoverview.ObjectHandlers::methodReference"))
        assertThat(Util.getMetaInfo(ClassHandlers()::methodReference), `is`("io.javalin.routeoverview.ClassHandlers::methodReference"))
        assertThat(Util.getMetaInfo(::standAloneMethod), `is`("io.javalin.routeoverview.TestRouteOverview_KotlinKt::standAloneMethod"))
    }

    @Test
    fun `lambda works`() {
        assertThat(Util.getMetaInfo({}), `is`("io.javalin.routeoverview.TestRouteOverview_Kotlin::??? (anonymous lambda)"))
    }

}

object ObjectHandlers {
    val lambdaField = Handler {}
    fun methodReference(ctx: Context) {}
    class ImplementingClass : Handler {
        override fun handle(context: Context) {}
    }
}

class ClassHandlers {
    val lambdaField = Handler {}
    fun methodReference(ctx: Context) {}
}
