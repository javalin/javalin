/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

@file:Suppress("DEPRECATION")

package io.javalin

import com.google.gson.GsonBuilder
import io.javalin.hook.HooktAlreadyExistsException
import io.javalin.hook.Hook
import io.javalin.hook.ParametrizedHook
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.testing.SerializableObject
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class TestHooks {

    private class MyOtherThing(val test: String = "Test")
    private val useMyOtherThing = Hook<MyOtherThing>("my-other-thing")

    @Test
    fun `register throws if hook already exists`() {
        assertThrows<HooktAlreadyExistsException> {
            Javalin.create {
                it.registerHook(useMyOtherThing, MyOtherThing())
                it.registerHook(useMyOtherThing, MyOtherThing())
            }
        }
    }

    @Test
    fun `hook can be accessed through the app`() = TestUtil.test(Javalin.create {
        it.registerHook(useMyOtherThing, MyOtherThing())
    }) { app, _ ->
        assertThat(app.unsafeConfig().pvt.hookManager.resolve(useMyOtherThing).test).isEqualTo("Test")
    }

    private class MyJson {
        fun render(obj: Any): String = GsonBuilder().create().toJson(obj)
    }
    private val useMyJson = Hook<MyJson>("my-json")

    @Test
    fun `hook can be accessed through the Context`() = TestUtil.test(Javalin.create {
        it.registerHook(useMyJson, MyJson())
    }) { app, http ->
        val gson = GsonBuilder().create()
        app.get("/") { it.result(it.use(useMyJson).render(SerializableObject())) }
        assertThat(http.getBody("/")).isEqualTo(gson.toJson(SerializableObject()))
    }

    @Test
    fun `use throws if hook does not exist`() = TestUtil.test(Javalin.create()) { app, http ->
        app.get("/") { it.result(it.use(useMyJson).render(SerializableObject())) }
        assertThat(http.get("/").status).isEqualTo(INTERNAL_SERVER_ERROR.code)
    }

    @Test
    fun `class can be used as hook key`() = TestUtil.test(Javalin.create {
        it.registerHook(MyOtherThing::class.java, MyOtherThing())
    }) { app, http ->
        app.get("/") { it.result(it.use(MyOtherThing::class.java).test) }
        assertThat(http.getBody("/")).isEqualTo("Test")
    }

    private class Database(val readOnly: Boolean)
    private class DatabaseParameters(var readOnlyTransaction: Boolean)
    private val useDatabase = ParametrizedHook<Database, DatabaseParameters>("use-database") { DatabaseParameters(readOnlyTransaction = false) }

    @Test
    fun `parametrized hook returns requested value`() = TestUtil.test(Javalin.create {
        it.pvt.hookManager.register(useDatabase) { cfg, _ -> Database(cfg.readOnlyTransaction) }
    }) { app, http ->
        app.get("/") { ctx ->
            ctx.result(
                app.unsafeConfig().pvt.hookManager.resolve(useDatabase, { it.readOnlyTransaction = true }, ctx).readOnly.toString()
            )
        }
        assertThat(http.getBody("/")).isEqualTo("true")
    }

}
