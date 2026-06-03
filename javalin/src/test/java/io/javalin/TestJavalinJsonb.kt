package io.javalin

import io.avaje.jsonb.Json
import io.avaje.jsonb.Jsonb
import io.javalin.http.bodyAsClass
import io.javalin.json.JavalinJsonb
import io.javalin.testing.SerializableObject
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

// JSON imports are in TestJavalinJsonb_Java.java so that the annotation processor sees them

internal class TestJavalinJsonb {

    fun appWithJsonb() = Javalin.create {
        it.jsonMapper(JavalinJsonb())
    }
    
    @Test
    fun `JavalinJsonb can convert a small Stream to JSON`() {
        TestJsonMapper.convertSmallStreamToJson(JavalinJsonb())
    }

    @Test
    fun `JavalinJsonb can convert a large Stream to JSON`() {
        TestJsonMapper.convertLargeStreamToJson(JavalinJsonb())
    }

    @Test
    fun `user can serialize objects using avaje-jsonb mapper`() = TestUtil.test(appWithJsonb()) { app, http ->
        app.unsafe.routes.get("/") { it.json(SerializableObject()) }
        assertThat(http.getBody("/")).isEqualTo(Jsonb.instance().toJson(SerializableObject()))
    }

    @Test
    fun `user can deserialize objects using avaje-jsonb mapper`() = TestUtil.test(appWithJsonb()) { app, http ->
        app.unsafe.routes.post("/") { it.result(it.bodyAsClass<SerializableObject>().value1) }
        assertThat(http.post("/").body(Jsonb.instance().toJson(SerializableObject())).asString().body).isEqualTo(SerializableObject().value1)
    }

    @Test
    fun `JavalinJsonb properly handles json stream`() = TestUtil.test(appWithJsonb()) { app, http ->
        app.unsafe.routes.get("/") { it.jsonStream(arrayOf("1")) }
        assertThat(http.get("/").body).isEqualTo("[\"1\"]")
    }

    @Test
    fun `toJsonStream treats Strings as already being json`() = TestUtil.test(appWithJsonb()) { app, http ->
        app.unsafe.routes.get("/") { it.jsonStream("{a:b}") }
        assertThat(http.getBody("/")).isEqualTo("{a:b}")
    }

    @Test
    fun `toJson treats Strings as already being json`() = TestUtil.test(appWithJsonb()) { app, http ->
        app.unsafe.routes.get("/") { it.json("{a:b}") }
        assertThat(http.getBody("/")).isEqualTo("{a:b}")
    }
}
