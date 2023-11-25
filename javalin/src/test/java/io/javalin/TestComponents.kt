/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.google.gson.GsonBuilder
import io.javalin.component.ComponentAccessor
import io.javalin.component.ConfigurableComponentAccessor
import io.javalin.testing.SerializableObject
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TestComponents {

    private class MyOtherThing(val test: String = "Test")
    private val useMyOtherThing = ComponentAccessor(MyOtherThing::class.java)

    @Test
    fun `components can be accessed through the app`() = TestUtil.test(Javalin.create {
        it.registerComponent(useMyOtherThing) { MyOtherThing() }
    }) { app, _ ->
        assertThat(app.component(useMyOtherThing).test).isEqualTo("Test")
    }

    private class MyJson {
        fun render(obj: Any): String = GsonBuilder().create().toJson(obj)
    }
    private val useMyJson = ComponentAccessor(MyJson::class.java)

    @Test
    fun `components can be accessed through the Context`() = TestUtil.test(Javalin.create {
        it.registerComponent(useMyJson) { MyJson() }
    }) { app, http ->
        val gson = GsonBuilder().create()
        app.get("/") { it.result(it.use(useMyJson).render(SerializableObject())) }
        assertThat(http.getBody("/")).isEqualTo(gson.toJson(SerializableObject()))
    }

    private class Database(val readOnly: Boolean)
    private class TestCfg(var readOnlyTransaction: Boolean)
    @Suppress("DEPRECATION")
    private val useDatabase = ConfigurableComponentAccessor(Database::class.java, { TestCfg(readOnlyTransaction = false) })

    @Suppress("DEPRECATION")
    @Test
    fun `configurable component returns requested component`() = TestUtil.test(Javalin.create {
        it.pvt.componentManager.registerResolver(useDatabase) { cfg, _ -> Database(cfg.readOnlyTransaction) }
    }) { app, http ->
        app.get("/") { ctx ->
            ctx.result(
                app.unsafeConfig().pvt.componentManager.resolve(useDatabase, { it.readOnlyTransaction = true }, ctx).readOnly.toString()
            )
        }
        assertThat(http.getBody("/")).isEqualTo("true")
    }

}
