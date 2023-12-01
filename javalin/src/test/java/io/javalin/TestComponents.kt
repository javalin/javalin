/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.google.gson.GsonBuilder
import io.javalin.component.Component
import io.javalin.http.use
import io.javalin.testing.SerializableObject
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TestComponents {

    private class MyOtherThing(val test: String = "Test") : Component

    @Test
    fun `components can be accessed through the app`() = TestUtil.test(Javalin.create {
        it.registerComponent(MyOtherThing())
    }) { app, _ ->
        assertThat(app.componentManager().get<MyOtherThing>().test).isEqualTo("Test")
    }

    private class MyJson : Component {
        fun render(obj: Any): String = GsonBuilder().create().toJson(obj)
    }

    @Test
    fun `components can be accessed through the Context`() = TestUtil.test(Javalin.create {
        it.registerComponent(MyJson())
    }) { app, http ->
        val gson = GsonBuilder().create()
        app.get("/") { it.result(it.use<MyJson>().render(SerializableObject())) }
        assertThat(http.getBody("/")).isEqualTo(gson.toJson(SerializableObject()))
    }

}
