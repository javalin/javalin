/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.json.JavalinJackson
import org.junit.jupiter.api.Test

internal class TestJavalinJackson {

    @Test
    fun `JavalinJackson can convert a small Stream to JSON`() {
        TestJsonMapper.convertSmallStreamToJson(JavalinJackson())
    }

    @Test
    fun `JavalinJackson can convert a large Stream to JSON`() {
        TestJsonMapper.convertLargeStreamToJson(JavalinJackson())
    }

}
