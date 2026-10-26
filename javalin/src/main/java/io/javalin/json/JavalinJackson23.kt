/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.json

import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream

/**
 * A [JsonMapper] that bridges Jackson 2 and Jackson 3, delegating each target type to whichever
 * version can actually process its annotations.
 *
 * Jackson 3 keeps the core annotations in the shared `com.fasterxml.jackson.annotation` package,
 * but moved its databind annotations to `tools.jackson.databind.annotation`. A type that carries a
 * Jackson 2 *databind* annotation (for example `com.fasterxml.jackson.databind.annotation.JsonDeserialize`)
 * is therefore only understood by Jackson 2, while everything else - types with no annotations, or
 * with only the shared `com.fasterxml.jackson.annotation` core annotations - is handled by Jackson 3.
 *
 * This lets an application move to Jackson 3 while still serving types (often coming from
 * third-party libraries) that remain annotated for Jackson 2, without having to rewrite them.
 *
 * By default [jackson2] is a [JavalinJackson] and [jackson3] is a [JavalinJackson3], but any
 * [JsonMapper] may be supplied - the routing only inspects the target type's annotations, so the
 * two delegates are never required on the classpath at the same time unless a type is actually
 * routed to each of them.
 *
 * Routing is decided from the raw target [Class]. Parameterized and container types (for example
 * `List<Foo>`) are handled by [jackson3]; when such a container holds Jackson 2 annotated elements,
 * stream them with [io.javalin.http.Context.writeJsonStream], which routes each element on its own.
 *
 * See [the discussion in issue #2571](https://github.com/javalin/javalin/issues/2571) for background.
 */
class JavalinJackson23 @JvmOverloads constructor(
    private val jackson2: JsonMapper = JavalinJackson(),
    private val jackson3: JsonMapper = JavalinJackson3(),
) : JsonMapper {

    /** Caches the routing decision per class to avoid repeating the annotation walk on hot paths. */
    private val requiresJackson2Cache = ConcurrentHashMap<Class<*>, Boolean>()

    override fun toJsonString(obj: Any, type: Type): String =
        mapperFor(type).toJsonString(obj, type)

    override fun toJsonStream(obj: Any, type: Type): InputStream =
        mapperFor(type).toJsonStream(obj, type)

    override fun <T : Any> fromJsonString(json: String, targetType: Type): T =
        mapperFor(targetType).fromJsonString(json, targetType)

    override fun <T : Any> fromJsonStream(json: InputStream, targetType: Type): T =
        mapperFor(targetType).fromJsonStream(json, targetType)

    override fun writeToOutputStream(stream: Stream<*>, outputStream: OutputStream) {
        // A stream may mix Jackson 2 and Jackson 3 types, so each element is routed on its own and
        // the surrounding JSON array is framed here rather than delegated to a single mapper.
        outputStream.write('['.code)
        val iterator = stream.iterator()
        while (iterator.hasNext()) {
            val element = iterator.next()
            val json = when (element) {
                null -> "null"
                else -> mapperFor(element.javaClass).toJsonString(element, element.javaClass)
            }
            outputStream.write(json.toByteArray(StandardCharsets.UTF_8))
            if (iterator.hasNext()) outputStream.write(','.code)
        }
        outputStream.write(']'.code)
    }

    private fun mapperFor(type: Type): JsonMapper =
        if (type is Class<*> && requiresJackson2(type)) jackson2 else jackson3

    private fun requiresJackson2(clazz: Class<*>): Boolean =
        requiresJackson2Cache.computeIfAbsent(clazz) { hasJackson2SpecificAnnotation(it) }

    companion object {
        /**
         * Returns true if [clazz] or any of its superclasses carries a Jackson 2 *specific*
         * annotation - one under `com.fasterxml.jackson` but outside the shared
         * `com.fasterxml.jackson.annotation` package that both Jackson versions understand.
         *
         * Superclasses are walked because code generators such as AutoValue emit a concrete
         * subclass (for example `AutoValue_Movie`) whose annotations live on the abstract parent.
         */
        private fun hasJackson2SpecificAnnotation(clazz: Class<*>): Boolean {
            var current: Class<*>? = clazz
            while (current != null && current != Any::class.java) {
                if (current.declaredAnnotations.any { it.isJackson2Specific() }) return true
                current = current.superclass
            }
            return false
        }

        private fun Annotation.isJackson2Specific(): Boolean {
            val name = annotationClass.java.name
            return name.startsWith("com.fasterxml.jackson.") &&
                !name.startsWith("com.fasterxml.jackson.annotation.")
        }
    }
}
