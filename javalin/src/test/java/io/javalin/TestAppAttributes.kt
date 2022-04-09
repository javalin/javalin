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
        attribute("MyJson", MyJson())
        attribute("MyOtherThing", MyOtherThing())
    }

    @Test
    fun `app attributes can be accessed through the app`() = TestUtil.test(attributedJavalin) { app, _ ->
        assertThat(app.attribute<MyOtherThing>("MyOtherThing").test).isEqualTo("Test")
    }

    @Test
    fun `app attributes can be accessed through the Context`() = TestUtil.test(attributedJavalin) { app, http ->
        val gson = GsonBuilder().create()
        app.get("/") { ctx ->
            val rendered = ctx.appAttribute<MyJson>("MyJson").render(SerializableObject())
            ctx.result(rendered)
        }
        assertThat(http.getBody("/")).isEqualTo(gson.toJson(SerializableObject()))
    }

}
