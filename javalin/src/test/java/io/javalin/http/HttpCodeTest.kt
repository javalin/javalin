package io.javalin.http

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HttpCodeTest {

    @Test
    fun `fetching http codes by code should get the right code`() {
        HttpCode.values().forEach {
            assertThat(HttpCode.forStatus(it.status)).isEqualTo(it)
        }
    }

    @Test
    fun `fetching http codes by code outside of range should not throw errors`() {
        assertThat(HttpCode.forStatus(-1)).isEqualTo(HttpCode.UNKNOWN)
        assertThat(HttpCode.forStatus(542345)).isEqualTo(HttpCode.UNKNOWN)
    }

    @Test
    fun `http code provides formatted implementation of toString() method`() {
        HttpCode.values().forEach {
            assertThat("${it.status} ${it.message}").isEqualTo(it.toString())
        }
    }

}
