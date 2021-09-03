/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.google.gson.GsonBuilder
import io.javalin.testing.SerializableObject
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestAppAttributes {

    class MyJson {
        fun render(obj: Any) = GsonBuilder().create().toJson(obj)
    }

    class MyOtherThing {
        val test = "Test"
    }

    private val attributedJavalin = Javalin.create().apply {
        attribute(MyJson::class.java.name, MyJson())
        attribute(MyOtherThing::class.java.name, MyOtherThing())
    }

    @Test
    fun `app attributes can be accessed through the app`() = TestUtil.test(attributedJavalin) { app, _ ->
        assertThat(app.attribute<MyOtherThing>(MyOtherThing::class.java.name).test).isEqualTo("Test")
    }

    @Test
    fun `app attributes can be accessed through the Context`() = TestUtil.test(attributedJavalin) { app, http ->
        val gson = GsonBuilder().create()
        app.get("/") { ctx ->
            val rendered = ctx.appAttribute<MyJson>(MyJson::class.java.name).render(SerializableObject())
            ctx.result(rendered)
        }
        assertThat(http.getBody("/")).isEqualTo(gson.toJson(SerializableObject()))
    }

}
