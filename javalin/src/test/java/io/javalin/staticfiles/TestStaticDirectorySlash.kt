/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 * @author: Plasmoxy
 */

package io.javalin.staticfiles

import io.javalin.Javalin
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.OK
import io.javalin.http.staticfiles.Location
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestStaticDirectorySlash {

    private val normalStaticFilesConfig = { cfg: io.javalin.config.JavalinConfig ->
        cfg.staticFiles.add("public", Location.CLASSPATH)
    }

    private val precompressingJavalin: Javalin by lazy {
        Javalin.create { cfg ->
            cfg.staticFiles.add {
                it.directory = "public"
                it.location = Location.CLASSPATH
                it.precompressMaxSize = 2 * 1024 * 1024
            }
        }
    }

    @Test
    fun `normal javalin ignores static directory slashes`() = testStaticFiles(normalStaticFilesConfig) { _, http ->
        assertThat(http.get("/subpage"))
            .extracting({ it.status }, { it.body })
            .containsExactly(OK.code, "TEST") // ok, is directory
        assertThat(http.get("/subpage/"))
            .extracting({ it.status }, { it.body })
            .containsExactly(OK.code, "TEST") // ok, is directory
    }

    @Test
    fun `precompressing javalin ignores static directory slashes`() = TestUtil.test(precompressingJavalin) { _, http ->
        assertThat(http.get("/subpage"))
            .extracting({ it.status }, { it.body })
            .containsExactly(OK.code, "TEST") // ok, is directory
        assertThat(http.get("/subpage/"))
            .extracting({ it.status }, { it.body })
            .containsExactly(OK.code, "TEST") // ok, is directory
    }


    @Test
    fun `normal Javalin serves files but serves directory if it is a directory`() = testStaticFiles(normalStaticFilesConfig) { _, http ->
        assertThat(http.get("/file"))
            .extracting({ it.status }, { it.body })
            .containsExactly(OK.code, "TESTFILE") // ok, is file = no slash
        assertThat(http.get("/file/"))
            .extracting({ it.status }, { it.body })
            .containsExactly(NOT_FOUND.code, "Endpoint GET /file/ not found") // nope, has slash must be directory
    }

    @Test
    fun `precompressing Javalin serves files but serves directory if it is a directory`() = TestUtil.test(precompressingJavalin) { _, http ->
        assertThat(http.get("/file"))
            .extracting({ it.status }, { it.body })
            .containsExactly(OK.code, "TESTFILE") // ok, is file = no slash
        assertThat(http.get("/file/"))
            .extracting({ it.status }, { it.body })
            .containsExactly(NOT_FOUND.code, "Endpoint GET /file/ not found") // nope, has slash must be directory
    }

}
