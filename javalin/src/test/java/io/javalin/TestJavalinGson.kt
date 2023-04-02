/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.google.gson.Gson
import io.javalin.http.bodyAsClass
import io.javalin.json.JavalinGson
import io.javalin.testing.SerializableObject
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class TestJavalinGson {

    @Test
    fun `JavalinGson can convert a small Stream to JSON`() {
        TestJsonMapper.convertSmallStreamToJson(JavalinGson())
    }

    @Test
    fun `JavalinGson can convert a large Stream to JSON`() {
        TestJsonMapper.convertLargeStreamToJson(JavalinGson())
    }

    @Test
    fun `user can serialize objects using gson mapper`() = TestUtil.test(Javalin.create {
        it.jsonMapper(JavalinGson())
    }) { app, http ->
        app.get("/") { it.json(SerializableObject()) }
        Assertions.assertThat(http.getBody("/")).isEqualTo(Gson().toJson(SerializableObject()))
    }

    @Test
    fun `user can deserialize objects using gson mapper`() = TestUtil.test(Javalin.create {
        it.jsonMapper(JavalinGson())
    }) { app, http ->
        app.post("/") { it.result(it.bodyAsClass<SerializableObject>().value1) }
        Assertions.assertThat(http.post("/").body(Gson().toJson(SerializableObject())).asString().body)
            .isEqualTo(SerializableObject().value1)
    }

}
