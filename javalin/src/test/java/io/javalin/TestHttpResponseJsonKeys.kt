/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.http.BadRequestResponse
import io.javalin.router.exception.HttpResponseExceptionMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestHttpResponseJsonKeys {
    @Test
    fun `JSON error detail keys round trip quotes backslashes and control characters`() {
        val key = "field\"\\\n" + (0..0x1f).map { it.toChar() }.joinToString("")
        val response = HttpResponseExceptionMapper.jsonResult(BadRequestResponse("bad input", mapOf(key to "invalid")))
        val parsed = ObjectMapper().readTree(response)
        assertThat(parsed["details"][key].asText()).isEqualTo("invalid")
        assertThat(parsed["details"].size()).isEqualTo(1)
    }
}
