/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.google.gson.GsonBuilder
import io.javalin.component.ComponentAccessor
import io.javalin.testing.SerializableObject
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TestComponents {

    private class MyJson {
        fun render(obj: Any): String = GsonBuilder().create().toJson(obj)
    }
    private val myJsonAccessor = ComponentAccessor(MyJson::class.java)

    private class MyOtherThing {
        val test = "Test"
    }
    private val myOtherThingAccessor = ComponentAccessor(MyOtherThing::class.java)

    private val attributedJavalin = Javalin.create {
        it.registerComponent(myJsonAccessor) { MyJson() }
        it.registerComponent(myOtherThingAccessor) { MyOtherThing() }
    }

    @Test
    fun `app attributes can be accessed through the app`() = TestUtil.test(attributedJavalin) { app, _ ->
        assertThat(app.unsafeConfig().pvt.componentManager.resolve(myOtherThingAccessor, null).test).isEqualTo("Test")
    }

    @Test
    fun `app attributes can be accessed through the Context`() = TestUtil.test(attributedJavalin) { app, http ->
        val gson = GsonBuilder().create()
        app.get("/") { it.result(it.use(myJsonAccessor).render(SerializableObject())) }
        assertThat(http.getBody("/")).isEqualTo(gson.toJson(SerializableObject()))
    }

}
