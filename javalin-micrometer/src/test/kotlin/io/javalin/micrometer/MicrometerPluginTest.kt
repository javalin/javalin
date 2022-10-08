package io.javalin.micrometer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MicrometerPluginTest {

    @Test
    fun `test that tests can be tested`() {
        assertThat(MicrometerPlugin().testable()).isEqualTo("Wrong")
    }

}
