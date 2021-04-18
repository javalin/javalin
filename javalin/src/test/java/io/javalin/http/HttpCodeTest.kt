package io.javalin.http

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class HttpCodeTest {

    @Test
    fun `fetching http codes by code should get the right code`() {
        HttpCode.values().forEach {
            assertThat(HttpCode.codeFor(it.status)).isEqualTo(it)
        }
    }

    @Test
    fun `fetching http codes by code outside of range should not throw errors`() {
        assertThat(HttpCode.codeFor(512)).isNull()
        assertThat(HttpCode.codeFor(-1)).isNull()
        assertThat(HttpCode.codeFor(542345)).isNull()
    }

}
