/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.json.JavalinGson
import org.junit.jupiter.api.Test

internal class TestJavalinGson {

    @Test
    fun `JavalinGson can convert a small Stream to JSON`() {
        TestJsonMapper.convertSmallStreamToJson(JavalinGson())
    }

    @Test
    fun `JavalinGson can convert a large Stream to JSON`() {
        TestJsonMapper.convertLargeStreamToJson(JavalinGson())
    }

}
