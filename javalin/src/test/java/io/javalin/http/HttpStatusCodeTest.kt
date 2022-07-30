package io.javalin.http

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HttpStatusCodeTest {

    @Test
    fun `fetching http codes by code should get the right code`() {
        HttpStatus.values().forEach {
            assertThat(HttpStatus.forStatus(it.status)).isEqualTo(it)
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
            assertThat("${it.status} ${it.message}").isEqualTo(it.toString())
        }
    }

    enum class CustomHttpCode(override val status: Int, override val message: String) : HttpStatusCode {
        CUSTOM(123, "Custom message")
    }

    @Test
    fun `creating a custom http code should work`() {
        assertThat(CustomHttpCode.CUSTOM.status).isEqualTo(123)
        assertThat(CustomHttpCode.CUSTOM.message).isEqualTo("Custom message")
    }


}
