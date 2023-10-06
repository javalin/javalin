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

    @Test
    fun `isInformational() should return true for informational status codes`() {
        assertHttpStatusCodeRange(100..199) {
            assertThat(it.isInformational()).isTrue()
        }
    }

    @Test
    fun `isSuccess() should return true for success status codes`() {
        assertHttpStatusCodeRange(200..299) {
            assertThat(it.isSuccess()).isTrue()
        }
    }

    @Test
    fun `isRedirection() should return true for redirection status codes`() {
        assertHttpStatusCodeRange(300..399) {
            assertThat(it.isRedirection()).isTrue()
        }
    }

    @Test
    fun `isClientError() and isError() should return true for client error status codes`() {
        assertHttpStatusCodeRange(400..499) {
            assertThat(it.isClientError()).isTrue()
            assertThat(it.isError()).isTrue()
        }
    }

    @Test
    fun `isServerError() and isError() should return true for server error status codes`() {
        assertHttpStatusCodeRange(500..599) {
            assertThat(it.isServerError()).isTrue()
            assertThat(it.isError()).isTrue()
        }
    }

    private fun assertHttpStatusCodeRange(range: IntRange, assertion: (HttpStatus) -> Unit) {
        range
            .map { HttpStatus.forStatus(it) }
            .filter { it != HttpStatus.UNKNOWN }
            .forEach { assertion(it) }
    }

}
