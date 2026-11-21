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

class TestHttpResponseJsonControls {
    @Test
    fun `JSON error titles and detail values round trip all control characters`() {
        val controls = (0..0x1f).map { it.toChar() }.joinToString("")
        val response = HttpResponseExceptionMapper.jsonResult(BadRequestResponse(controls, mapOf("controls" to controls)))
        val parsed = ObjectMapper().readTree(response)
        assertThat(parsed["title"].asText()).isEqualTo(controls)
        assertThat(parsed["details"]["controls"].asText()).isEqualTo(controls)
    }
}
