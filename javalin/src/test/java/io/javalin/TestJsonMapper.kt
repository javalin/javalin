/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.json.JsonMapper
import org.assertj.core.api.Assertions.assertThat
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.streams.asStream

class TestJsonMapper {

    companion object {

        fun convertSmallStreamToJson(jsonMapper: JsonMapper) {
            data class Foo(val value: Long)

            val source = listOf(Foo(1_000_000), Foo(1_000_001))
            val baos = ByteArrayOutputStream()
            jsonMapper.writeToOutputStream(source.stream(), baos)
            assertThat("""[{"value":1000000},{"value":1000001}]""").isEqualTo(baos.toString())
        }

        fun convertLargeStreamToJson(jsonMapper: JsonMapper) {
            data class Foo(val value: Long)

            val countingOutputStream = object : OutputStream() {
                var count: Long = 0
                override fun write(b: Int) {
                    count++
                }
            }
            var value = 1_000_000_000L
            val take = 50_000_000
            val seq = generateSequence { Foo(value++) }
            jsonMapper.writeToOutputStream(seq.take(take).asStream(), countingOutputStream)
            // expectedCharacterCount is approximately 1GB
            val expectedCharacterCount = 2 + // bookend brackets
                (take - 1) + // commas
                20 * take // objects {"value":1000000000}
            assertThat(expectedCharacterCount).isEqualTo(countingOutputStream.count)
        }

    }

}
