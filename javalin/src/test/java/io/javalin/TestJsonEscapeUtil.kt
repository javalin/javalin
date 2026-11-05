/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.util.JsonEscapeUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestJsonEscapeUtil {

    @Test
    fun `escape covers every JSON control character`() {
        val shortEscapes = mapOf('\b' to "\\b", '\t' to "\\t", '\n' to "\\n", '\u000c' to "\\f", '\r' to "\\r")
        for (code in 0..0x1f) {
            val character = code.toChar()
            val expected = shortEscapes[character] ?: "\\u" + code.toString(16).padStart(4, '0')
            assertThat(JsonEscapeUtil.escape(character.toString())).isEqualTo(expected)
        }
    }

    @Test
    fun `escape preserves ordinary and non-ASCII text`() {
        val text = "ordinary / text 한국어 😀"
        assertThat(JsonEscapeUtil.escape(text)).isEqualTo(text)
    }

    @Test
    fun `escape preserves existing quote and backslash escaping`() {
        assertThat(JsonEscapeUtil.escape("\"\\")).isEqualTo("\\\"\\\\")
    }
}
