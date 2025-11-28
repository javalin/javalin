/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.google.gson.GsonBuilder
import io.javalin.config.Key
import io.javalin.config.KeyAlreadyExistsException
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.testing.SerializableObject
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class TestAppData {

    private class MyOtherThing(val test: String = "Test")
    private val myOtherKey = Key<MyOtherThing>("my-other-thing")

    @Test
    fun `register throws if key already exists`() {
        assertThrows<KeyAlreadyExistsException> {
            Javalin.create {
                it.appData(myOtherKey, MyOtherThing())
                it.appData(myOtherKey, MyOtherThing())
            }
        }
    }

    @Test
    fun `register throws if id of key already exists`() {
        assertThrows<KeyAlreadyExistsException> {
            Javalin.create {
                it.appData(Key<Short>("A"), 1)
                it.appData(Key<Long>("A"), 1)
            }
        }
    }

    @Test
    fun `data can be accessed through the app`() = TestUtil.test(Javalin.create {
        it.appData(myOtherKey, MyOtherThing())
    }) { app, _ ->
        assertThat(app.unsafe.appDataManager.get(myOtherKey).test).isEqualTo("Test")
    }

    private class MyJson {
        fun render(obj: Any): String = GsonBuilder().create().toJson(obj)
    }
    private val myJsonKey = Key<MyJson>("my-json")

    @Test
    fun `app data can be accessed through the Context`() = TestUtil.test(Javalin.create { config ->
        config.appData(myJsonKey, MyJson())
        config.routes.get("/") { it.result(it.appData(myJsonKey).render(SerializableObject())) }
    }) { app, http ->
        val gson = GsonBuilder().create()
        assertThat(http.getBody("/")).isEqualTo(gson.toJson(SerializableObject()))
    }

    @Test
    fun `Context#appData() throws if data does not exist`() = TestUtil.test(Javalin.create { config ->
        config.routes.get("/") { it.result(it.appData(myJsonKey).render(SerializableObject())) }
    }) { app, http ->
        assertThat(http.get("/").status).isEqualTo(INTERNAL_SERVER_ERROR.code)
    }

    @Test
    fun `keys can be used without storing them as fields()`() = TestUtil.test(Javalin.create {
        it.appData(Key("key-equality"), MyOtherThing())
    }) { app, http ->
        app.unsafe.routes.get("/") { it.result(it.appData(Key<MyOtherThing>("key-equality")).test) }
        assertThat(http.getBody("/")).isEqualTo("Test")
    }

}
