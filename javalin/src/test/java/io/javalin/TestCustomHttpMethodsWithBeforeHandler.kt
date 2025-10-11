/*
 * Javalin - https://javalin.io
 * Copyright 2024 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.HttpStatus
import io.javalin.testing.TestUtil
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests for using before-handlers to handle custom HTTP methods (e.g., WebDAV methods)
 */
class TestCustomHttpMethodsWithBeforeHandler {

    @Test
    fun `custom HTTP method PROPFIND works with before handler`() = TestUtil.test { app, http ->
        app.before("/dav/*") { ctx ->
            if (ctx.req().method == "PROPFIND") {
                ctx.result("PROPFIND response")
                ctx.skipRemainingHandlers()
            }
        }
        
        val response = Unirest.request("PROPFIND", http.origin + "/dav/folder").asString()
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(response.body).isEqualTo("PROPFIND response")
    }
}
