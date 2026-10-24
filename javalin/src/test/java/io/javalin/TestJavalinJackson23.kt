/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.json.JavalinJackson23
import io.javalin.json.JsonMapper
import io.javalin.json.fromJsonString
import io.javalin.json.toJsonString
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.lang.reflect.Type
import com.fasterxml.jackson.annotation.JsonRootName as Jackson2SharedAnnotation
import com.fasterxml.jackson.databind.annotation.JsonDeserialize as Jackson2DatabindAnnotation

internal class TestJavalinJackson23 {

    // --- Types used to exercise the routing decision ------------------------------------------

    private class PlainType(val value: String = "plain")

    @Jackson2SharedAnnotation("shared") // com.fasterxml.jackson.annotation - understood by both
    private class SharedAnnotatedType(val value: String = "shared")

    @Jackson2DatabindAnnotation // com.fasterxml.jackson.databind.annotation - Jackson 2 only
    private class Jackson2AnnotatedType(val value: String = "j2")

    @Jackson2DatabindAnnotation
    private open class Jackson2AnnotatedParent(val value: String = "parent")

    // Mirrors AutoValue: the concrete subclass has no annotation, the parent does.
    private class GeneratedSubclass : Jackson2AnnotatedParent()

    /** Records which delegate was asked and returns the tag so routing is observable in assertions. */
    private class TaggingMapper(private val tag: String) : JsonMapper {
        override fun toJsonString(obj: Any, type: Type): String = "\"$tag\""
        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> fromJsonString(json: String, targetType: Type): T = tag as T
    }

    private val jackson2 = TaggingMapper("J2")
    private val jackson3 = TaggingMapper("J3")
    private val bridge = JavalinJackson23(jackson2, jackson3)

    // --- Routing tests -------------------------------------------------------------------------

    @Test
    fun `types without annotations are routed to Jackson 3`() {
        assertThat(bridge.toJsonString(PlainType())).isEqualTo("\"J3\"")
    }

    @Test
    fun `types with only shared core annotations are routed to Jackson 3`() {
        // This is the key correctness point: a common annotation like @JsonProperty / @JsonRootName
        // must not force the type onto Jackson 2, since Jackson 3 understands it too.
        assertThat(bridge.toJsonString(SharedAnnotatedType())).isEqualTo("\"J3\"")
    }

    @Test
    fun `types with Jackson 2 databind annotations are routed to Jackson 2`() {
        assertThat(bridge.toJsonString(Jackson2AnnotatedType())).isEqualTo("\"J2\"")
    }

    @Test
    fun `Jackson 2 annotations on a superclass route the subclass to Jackson 2`() {
        assertThat(bridge.toJsonString(GeneratedSubclass())).isEqualTo("\"J2\"")
    }

    @Test
    fun `fromJsonString is routed by the target type annotations`() {
        assertThat(bridge.fromJsonString<PlainType>("{}")).isEqualTo("J3")
        assertThat(bridge.fromJsonString<Jackson2AnnotatedType>("{}")).isEqualTo("J2")
    }

    // --- Stream tests --------------------------------------------------------------------------

    @Test
    fun `writeToOutputStream routes each element independently`() {
        val baos = ByteArrayOutputStream()
        bridge.writeToOutputStream(listOf(PlainType(), Jackson2AnnotatedType()).stream(), baos)
        assertThat(baos.toString()).isEqualTo("""["J3","J2"]""")
    }

    @Test
    fun `writeToOutputStream renders null elements`() {
        val baos = ByteArrayOutputStream()
        bridge.writeToOutputStream(listOf(PlainType(), null).stream(), baos)
        assertThat(baos.toString()).isEqualTo("""["J3",null]""")
    }

    @Test
    fun `default JavalinJackson23 can convert a small Stream to JSON`() {
        TestJsonMapper.convertSmallStreamToJson(JavalinJackson23())
    }

    @Test
    fun `default JavalinJackson23 can convert a large Stream to JSON`() {
        TestJsonMapper.convertLargeStreamToJson(JavalinJackson23())
    }

    // --- End-to-end tests using the real Jackson delegates -------------------------------------

    data class SerializableDataClass(val value1: String = "Default1", val value2: String)

    @Test
    fun `default JavalinJackson23 round-trips a kotlin data class`() {
        val mapper = JavalinJackson23()
        val mapped = mapper.toJsonString(SerializableDataClass("First value", "Second value"))
        val mappedBack = mapper.fromJsonString<SerializableDataClass>(mapped)
        assertThat(mappedBack.value1).isEqualTo("First value")
        assertThat(mappedBack.value2).isEqualTo("Second value")
    }

    @Test
    fun `default JavalinJackson23 serves JSON over HTTP`() = TestUtil.test(Javalin.create {
        it.jsonMapper(JavalinJackson23())
    }) { app, http ->
        app.unsafe.routes.get("/") { it.json(SerializableDataClass("a", "b")) }
        assertThat(http.getBody("/")).isEqualTo("""{"value1":"a","value2":"b"}""")
    }
}
