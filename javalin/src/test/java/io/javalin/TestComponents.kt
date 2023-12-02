/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

@file:Suppress("DEPRECATION")

package io.javalin

import com.google.gson.GsonBuilder
import io.javalin.component.ComponentAlreadyExistsException
import io.javalin.component.ComponentHandle
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.testing.SerializableObject
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class TestComponents {

    private class MyOtherThing(val test: String = "Test")
    private val myOtherThingHandle = ComponentHandle<MyOtherThing>()

    @Test
    fun `register throws if component already exists`() {
        assertThrows<ComponentAlreadyExistsException> {
            Javalin.create {
                it.registerComponent(myOtherThingHandle, MyOtherThing())
                it.registerComponent(myOtherThingHandle, MyOtherThing())
            }
        }
        assertThrows<ComponentAlreadyExistsException> {
            Javalin.create {
                it.registerComponent(MyOtherThing::class, MyOtherThing())
                it.registerComponent(MyOtherThing::class, MyOtherThing())
            }
        }
        assertThrows<ComponentAlreadyExistsException> {
            Javalin.create {
                it.registerComponent(MyOtherThing::class.java, MyOtherThing())
                it.registerComponent(MyOtherThing::class.java, MyOtherThing())
            }
        }
        assertThrows<ComponentAlreadyExistsException> {
            Javalin.create {
                it.registerComponent(MyOtherThing::class, MyOtherThing())
                it.registerComponent(MyOtherThing::class.java, MyOtherThing())
            }
        }
    }

    @Test
    fun `components can be accessed through the app`() = TestUtil.test(Javalin.create {
        it.registerComponent(myOtherThingHandle, MyOtherThing())
        it.registerComponent(MyOtherThing::class, MyOtherThing())
    }) { app, _ ->
        assertThat(app.component(myOtherThingHandle).test).isEqualTo("Test")
        assertThat(app.component(MyOtherThing::class).test).isEqualTo("Test")
        assertThat(app.component(MyOtherThing::class.java).test).isEqualTo("Test")
    }

    private class MyJson {
        fun render(obj: Any): String = GsonBuilder().create().toJson(obj)
    }
    private val useMyJson = ComponentHandle<MyJson>()
    private val useResolvedComponent = ComponentHandle<String?>()

    @Test
    fun `components can be accessed through the Context`() = TestUtil.test(Javalin.create {
        it.registerComponent(MyJson::class, MyJson())
        it.registerComponent(useMyJson, MyJson())
        it.registerComponentResolver(useResolvedComponent) { ctx -> ctx?.path() }
    }) { app, http ->
        val gson = GsonBuilder().create()
        app.get("/handle") { it.result(it.use(useMyJson).render(SerializableObject())) }
        assertThat(http.getBody("/handle")).isEqualTo(gson.toJson(SerializableObject()))
        app.get("/kclass") { it.result(it.use(MyJson::class).render(SerializableObject())) }
        assertThat(http.getBody("/kclass")).isEqualTo(gson.toJson(SerializableObject()))
        app.get("/class") { it.result(it.use(MyJson::class.java).render(SerializableObject())) }
        assertThat(http.getBody("/class")).isEqualTo(gson.toJson(SerializableObject()))
        app.get("/resolved") { it.result(it.use(useResolvedComponent)!!) }
        assertThat(http.getBody("/resolved")).isEqualTo("/resolved")
    }

    @Test
    fun `use throws if component does not exist`() = TestUtil.test(Javalin.create()) { app, http ->
        app.get("/") { it.result(it.use(useMyJson).render(SerializableObject())) }
        assertThat(http.get("/").status).isEqualTo(INTERNAL_SERVER_ERROR.code)
    }
}
