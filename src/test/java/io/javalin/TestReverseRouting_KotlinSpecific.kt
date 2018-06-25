/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.reverserouting.ImplementingClass
import io.javalin.reverserouting.SomeController
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class TestReverseRouting_KotlinSpecific : _UnirestBaseTest() {

    private val helloHandler = Handler { ctx -> ctx.result("Hello World") }

    @Test
    fun test_pathFinder_works_field() {
        _UnirestBaseTest.app.get("/hello-get", helloHandler)
        assertThat(_UnirestBaseTest.app.pathBuilder(helloHandler).build(), `is`("/hello-get"))
    }

    @Test
    fun test_pathFinder_works_methodRef() {
        _UnirestBaseTest.app.get("/hello-get", SomeController::methodRef)
        assertThat(_UnirestBaseTest.app.pathBuilder(SomeController::methodRef).build(), `is`("/hello-get"))
    }

    @Test
    fun test_pathFinder_works_implementingClass() {
        val implementingClass = ImplementingClass()
        _UnirestBaseTest.app.get("/hello-get", implementingClass)
        assertThat(_UnirestBaseTest.app.pathBuilder(implementingClass).build(), `is`("/hello-get"))
    }

}
