package io.javalin.http

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HttpCodeTest {

    @Test
    fun `fetching http codes by code should get the right code`() {
        HttpCodes.values().forEach {
            assertThat(HttpCodes.forStatus(it.status)).isEqualTo(it)
        }
    }

    @Test
    fun `fetching http codes by code outside of range should not throw errors`() {
        assertThat(HttpCodes.forStatus(-1)).isEqualTo(HttpCodes.UNKNOWN)
        assertThat(HttpCodes.forStatus(542345)).isEqualTo(HttpCodes.UNKNOWN)
    }

    @Test
    fun `http code provides formatted implementation of toString() method`() {
        HttpCodes.values().forEach {
            assertThat("${it.status} ${it.message}").isEqualTo(it.toString())
        }
    }

    enum class CustomHttpCode(override val status: Int, override val message: String) : HttpCode {
        CUSTOM(123, "Custom message")
    }

    @Test
    fun `creating a custom http code should work`() {
        assertThat(CustomHttpCode.CUSTOM.status).isEqualTo(123)
        assertThat(CustomHttpCode.CUSTOM.message).isEqualTo("Custom message")
    }


}
