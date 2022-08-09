/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.routeoverview

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.bundled.RouteOverviewUtil.metaInfo
import io.javalin.routeoverview.VisualTest.HandlerImplementation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

// class/object/companion object
// fields/properties, both standalone and within a class/object
// functions/methods, both bound and unbound

val standAloneField = Handler {}
fun standAloneMethod(ctx: Context) {}

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

internal class TestRouteOverviewInKotlin {

    @Test
    fun `field works`() {
        assertThat(ObjectHandlers.lambdaField.metaInfo).isIn(
            setOf(
                "io.javalin.routeoverview.ClassHandlers::??? (anonymous lambda)", // JDK >= 15
                "io.javalin.routeoverview.ObjectHandlers.lambdaField", // JDK < 15
            )
        )
        assertThat(ClassHandlers().lambdaField.metaInfo).isIn(
            setOf(
                "io.javalin.routeoverview.ClassHandlers.lambdaField",
                "io.javalin.routeoverview.ClassHandlers::??? (anonymous lambda)"
            )
        )
        assertThat(standAloneField.metaInfo).isEqualTo("io.javalin.routeoverview.TestRouteOverviewInKotlinKt.standAloneField")
    }

    @Test
    fun `class works`() {
        assertThat(ObjectHandlers.ImplementingClass().metaInfo).isEqualTo("io.javalin.routeoverview.ObjectHandlers\$ImplementingClass.class")
        assertThat(HandlerImplementation().metaInfo).isEqualTo("io.javalin.routeoverview.VisualTest\$HandlerImplementation.class")
    }

    @Test
    fun `method reference works`() { // this is ridiculous...
        assertThat(ObjectHandlers::methodReference.metaInfo).isIn(
            setOf(
                "io.javalin.routeoverview.TestRouteOverviewInKotlin::??? (anonymous lambda)", // JDK >= 15
                "io.javalin.routeoverview.ObjectHandlers::??? (anonymous lambda)", // Kotlin >= 1.5
                "io.javalin.routeoverview.ObjectHandlers::methodReference" // Kotlin < 1.5
            )
        )
        assertThat(ClassHandlers()::methodReference.metaInfo).isIn(
            setOf(
                "io.javalin.routeoverview.TestRouteOverviewInKotlin::??? (anonymous lambda)", // JDK >= 15
                "io.javalin.routeoverview.ClassHandlers::??? (anonymous lambda)", // Kotlin >= 1.5
                "io.javalin.routeoverview.ClassHandlers::methodReference" // Kotlin < 1.5
            )
        )
        assertThat(::standAloneMethod.metaInfo).isIn(
            setOf(
                "io.javalin.routeoverview.TestRouteOverviewInKotlin::??? (anonymous lambda)", // Kotlin >= 1.5
                "io.javalin.routeoverview.TestRouteOverviewInKotlinKt::standAloneMethod" // Kotlin < 1.5
            )
        )
    }

    @Test
    fun `lambda works`() {
        assertThat({}.metaInfo).isEqualTo("io.javalin.routeoverview.TestRouteOverviewInKotlin::??? (anonymous lambda)")
    }

}
