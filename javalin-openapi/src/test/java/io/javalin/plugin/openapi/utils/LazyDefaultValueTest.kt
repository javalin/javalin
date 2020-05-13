package io.javalin.plugin.openapi.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LazyDefaultValueTest {

    @Test
    fun `should init default value if not overridden`() {
        var supplierCalled = false
        val sideEffectSupplier: (() -> Int) = {
            supplierCalled = true
            0
        }
        val value by LazyDefaultValue { sideEffectSupplier() }
        assertThat(value).isEqualTo(0)
        assertThat(supplierCalled).isTrue()
    }

    @Test
    fun `should not call init function if overridden`() {
        var supplierCalled = false
        val sideEffectSupplier: (() -> Int) = {
            supplierCalled = true
            0
        }
        var value by LazyDefaultValue { sideEffectSupplier() }
        value = 1
        assertThat(value).isEqualTo(1)
        assertThat(supplierCalled).isFalse()
    }
}
