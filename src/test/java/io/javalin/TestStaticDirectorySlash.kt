/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 * @author: Plasmoxy
 */

package io.javalin

import io.javalin.util.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestStaticDirectorySlash {

    private val normalJavalin = Javalin.create()
            .enableStaticFiles("public")

    private val nonIgnoringJavalin = Javalin.create()
            .enableStaticFiles("public")
            .dontIgnoreTrailingSlashes()

    @Test
    fun `normal javalin ignores static directory slashes`() = TestUtil.test(normalJavalin) { _, http ->
        assertThat(http.getBody("/subpage")).isEqualTo("TEST") // ok, is directory
        assertThat(http.getBody("/subpage/")).isEqualTo("TEST") // ok, is directory
    }

    @Test
    fun `nonIgnoringJavalin (dontIgnoreTrailingSlashes()) doesn't ignore static file slashes`() = TestUtil.test(nonIgnoringJavalin) { _, http ->
        assertThat(http.getBody("/subpage")).isEqualTo("Not found") // nope, non ignoring and doesnt have slash
        assertThat(http.getBody("/subpage/")).isEqualTo("TEST") // ok, has slash
    }

    @Test
    fun `normal Javalin serves files but serves directory if it is a directory`() = TestUtil.test(normalJavalin) { _, http ->
        assertThat(http.getBody("/file")).isEqualTo("TESTFILE") // ok, is file = no slash
        assertThat(http.getBody("/file/")).isEqualTo("Not found") // nope, has slash must be directory
    }

    @Test
    fun `nonIgnoring Javalin serves files but serves directory if it is a directory`() = TestUtil.test(nonIgnoringJavalin) { _, http ->
        assertThat(http.getBody("/file")).isEqualTo("TESTFILE") // ok, is file = no slash
        assertThat(http.getBody("/file/")).isEqualTo("Not found") // nope, has slash must be directory
    }

}
