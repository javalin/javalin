package io.javalin.openapi.utils

import io.javalin.plugin.openapi.utils.LazyDefaultValue
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.function.Supplier

class LazyDefaultValueTest {
    @Test
    fun `should init default value if not overriden`() {
        val mockSupplier = mockk<Supplier<Int>>()
        every { mockSupplier.get() } returns 0

        class MyClass {
            var value by LazyDefaultValue { mockSupplier.get() }
        }

        val myObject = MyClass()

        assertThat(myObject.value).isEqualTo(0)
    }

    @Test
    fun `should not call init function if overriden`() {
        val mockSupplier = mockk<Supplier<Int>>()
        every { mockSupplier.get() } returns 0

        class MyClass {
            var value by LazyDefaultValue { mockSupplier.get() }
        }

        val myObject = MyClass()
        myObject.value = 1

        assertThat(myObject.value).isEqualTo(1)
        verify(exactly = 0) { mockSupplier.get() }
        confirmVerified(mockSupplier)
    }
}
