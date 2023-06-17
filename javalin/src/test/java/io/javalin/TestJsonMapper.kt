/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.json.JsonMapper
import io.javalin.json.toJsonString
import org.assertj.core.api.Assertions.assertThat
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.streams.asStream

class TestJsonMapper {

    companion object {

        data class Foo(val value: Long)

        fun convertSmallStreamToJson(jsonMapper: JsonMapper) {
            val source = listOf(Foo(1_000_000), Foo(1_000_001))
            val baos = ByteArrayOutputStream()
            jsonMapper.writeToOutputStream(source.stream(), baos)
            assertThat("""[{"value":1000000},{"value":1000001}]""").isEqualTo(baos.toString())
        }

        fun convertLargeStreamToJson(jsonMapper: JsonMapper) {
            val countingOutputStream = CountingOutputStream()
            var valueLength = 1_000_000L // we will increment this up 1_050_000L
            val oneElementLength = jsonMapper.toJsonString(Foo(valueLength)).length
            val numElements = 50_000
            val seq = generateSequence { Foo(valueLength++) }
            jsonMapper.writeToOutputStream(seq.take(numElements).asStream(), countingOutputStream)
            val expectedCharacterCount = 2 + // bookend brackets
                (numElements - 1) + // commas
                oneElementLength * numElements // elements
            assertThat(expectedCharacterCount).isEqualTo(countingOutputStream.count)
        }

    }

    private class CountingOutputStream : OutputStream() {
        var count: Long = 0
        override fun write(b: Int) {
            count++
        }
    }

}
