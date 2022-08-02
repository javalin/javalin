package io.javalin.http

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HttpStatusCodeTest {

    @Test
    fun `fetching http codes by code should get the right code`() {
        HttpStatus.values().forEach {
            assertThat(HttpStatus.forStatus(it.code)).isEqualTo(it)
        }
    }

    @Test
    fun `fetching http codes by code outside of range should not throw errors`() {
        assertThat(HttpStatus.forStatus(-1)).isEqualTo(HttpStatus.UNKNOWN)
        assertThat(HttpStatus.forStatus(542345)).isEqualTo(HttpStatus.UNKNOWN)
    }

    @Test
    fun `http code provides formatted implementation of toString() method`() {
        HttpStatus.values().forEach {
            assertThat("${it.code} ${it.message}").isEqualTo(it.toString())
        }
    }

}
