/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 * @author: Plasmoxy
 */

package io.javalin.staticfiles

import io.javalin.Javalin
import io.javalin.http.HttpCode
import io.javalin.http.HttpCode.NOT_FOUND
import io.javalin.http.staticfiles.Location
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestStaticDirectorySlash {

    private val normalJavalin: Javalin by lazy { Javalin.create { it.staticFiles.add("public", Location.CLASSPATH) } }

    @Test
    fun `normal javalin ignores static directory slashes`() = TestUtil.test(normalJavalin) { _, http ->
        assertThat(http.getBody("/subpage")).isEqualTo("TEST") // ok, is directory
        assertThat(http.getBody("/subpage/")).isEqualTo("TEST") // ok, is directory
    }

    @Test
    fun `normal Javalin serves files but serves directory if it is a directory`() = TestUtil.test(normalJavalin) { _, http ->
        assertThat(http.getBody("/file")).isEqualTo("TESTFILE") // ok, is file = no slash
        assertThat(http.getBody("/file/")).isEqualTo(NOT_FOUND.message) // nope, has slash must be directory
    }

}
