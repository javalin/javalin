/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.routeoverview

import io.javalin.http.Context
import io.javalin.http.Handler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

// class/object/companion object
// fields/properties, both standalone and within a class/object
// functions/methods, both bound and unbound

val standAloneField = Handler {}
fun standAloneMethod(ctx: Context) {}

class TestRouteOverview_Kotlin {

    @Test
    fun `field works`() {
        assertThat(Util.getMetaInfo(ObjectHandlers.lambdaField)).isIn(setOf(
                "io.javalin.routeoverview.ClassHandlers::??? (anonymous lambda)", // JDK >= 15
                "io.javalin.routeoverview.ObjectHandlers.lambdaField", // JDK < 15
        ))
        assertThat(Util.getMetaInfo(ClassHandlers().lambdaField)).isIn(setOf(
                "io.javalin.routeoverview.ClassHandlers.lambdaField",
                "io.javalin.routeoverview.ClassHandlers::??? (anonymous lambda)"
        ))
        assertThat(Util.getMetaInfo(standAloneField)).isEqualTo("io.javalin.routeoverview.TestRouteOverview_KotlinKt.standAloneField")
    }

    @Test
    fun `class works`() {
        assertThat(Util.getMetaInfo(ObjectHandlers.ImplementingClass())).isEqualTo("io.javalin.routeoverview.ObjectHandlers\$ImplementingClass.class")
        assertThat(Util.getMetaInfo(HandlerImplementation())).isEqualTo("io.javalin.routeoverview.HandlerImplementation.class")
    }

    @Test
    fun `method reference works`() { // this is ridiculous...
        assertThat(Util.getMetaInfo(ObjectHandlers::methodReference)).isIn(setOf(
                "io.javalin.routeoverview.TestRouteOverview_Kotlin::??? (anonymous lambda)", // JDK >= 15
                "io.javalin.routeoverview.ObjectHandlers::??? (anonymous lambda)", // Kotlin >= 1.5
                "io.javalin.routeoverview.ObjectHandlers::methodReference" // Kotlin < 1.5
        ))
        assertThat(Util.getMetaInfo(ClassHandlers()::methodReference)).isIn(setOf(
                "io.javalin.routeoverview.TestRouteOverview_Kotlin::??? (anonymous lambda)", // JDK >= 15
                "io.javalin.routeoverview.ClassHandlers::??? (anonymous lambda)", // Kotlin >= 1.5
                "io.javalin.routeoverview.ClassHandlers::methodReference" // Kotlin < 1.5
        ))
        assertThat(Util.getMetaInfo(::standAloneMethod)).isIn(setOf(
                "io.javalin.routeoverview.TestRouteOverview_Kotlin::??? (anonymous lambda)", // Kotlin >= 1.5
                "io.javalin.routeoverview.TestRouteOverview_KotlinKt::standAloneMethod" // Kotlin < 1.5
        ))
    }

    @Test
    fun `lambda works`() {
        assertThat(Util.getMetaInfo({})).isEqualTo("io.javalin.routeoverview.TestRouteOverview_Kotlin::??? (anonymous lambda)")
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
