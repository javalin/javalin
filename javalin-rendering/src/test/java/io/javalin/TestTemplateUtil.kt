/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.rendering.template.TemplateUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertThrows


class TestTemplateUtil {
    @Test
    fun `model works`() {
        val model = TemplateUtil.model("foo", "bar", "baz", "qux")
        assertThat(model).isEqualTo(mapOf("foo" to "bar", "baz" to "qux"))
    }

    @Test
    fun `model throws exception if number of arguments is odd`() {
        assertThrows(IllegalArgumentException::class.java) {
            TemplateUtil.model("foo", "bar", "baz")
        }
    }

    @Test
    fun `model works with null values`() {
        val model = TemplateUtil.model("foo", "bar", "baz", null)
        assertThat(model).isEqualTo(mapOf("foo" to "bar", "baz" to null))
    }
}
