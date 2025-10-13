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
 * Tests for custom HTTP method routing via addHttpHandler(String, ...)
 */
class TestCustomHttpMethodHandler {

    @Test
    fun `PROPFIND method works through routing system`() = TestUtil.test { app, http ->
        app.addHttpHandler("PROPFIND", "/dav/*") { ctx -> 
            ctx.result("PROPFIND response")
        }
        
        val response = Unirest.request("PROPFIND", http.origin + "/dav/folder").asString()
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(response.body).isEqualTo("PROPFIND response")
    }

    @Test
    fun `MKCOL method works through routing system`() = TestUtil.test { app, http ->
        app.addHttpHandler("MKCOL", "/dav/*") { ctx ->
            ctx.result("MKCOL response").status(HttpStatus.CREATED)
        }
        
        val response = Unirest.request("MKCOL", http.origin + "/dav/newcollection").asString()
        assertThat(response.status).isEqualTo(HttpStatus.CREATED.code)
        assertThat(response.body).isEqualTo("MKCOL response")
    }

    @Test
    fun `can access path parameters in custom method handler`() = TestUtil.test { app, http ->
        app.addHttpHandler("PROPFIND", "/dav/{path}") { ctx ->
            ctx.result("Path: " + ctx.pathParam("path"))
        }
        
        val response = Unirest.request("PROPFIND", http.origin + "/dav/myfile.txt").asString()
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(response.body).isEqualTo("Path: myfile.txt")
    }

    @Test
    fun `method names are case insensitive`() = TestUtil.test { app, http ->
        app.addHttpHandler("propfind", "/dav/*") { ctx ->
            ctx.result("PROPFIND response")
        }
        
        val response = Unirest.request("PROPFIND", http.origin + "/dav/folder").asString()
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(response.body).isEqualTo("PROPFIND response")
    }

    @Test
    fun `multiple custom methods on same path work correctly`() = TestUtil.test { app, http ->
        app.addHttpHandler("PROPFIND", "/dav/*") { ctx -> ctx.result("PROPFIND") }
        app.addHttpHandler("MKCOL", "/dav/*") { ctx -> ctx.result("MKCOL") }
        app.addHttpHandler("COPY", "/dav/*") { ctx -> ctx.result("COPY") }
        
        assertThat(Unirest.request("PROPFIND", http.origin + "/dav/test").asString().body).isEqualTo("PROPFIND")
        assertThat(Unirest.request("MKCOL", http.origin + "/dav/test").asString().body).isEqualTo("MKCOL")
        assertThat(Unirest.request("COPY", http.origin + "/dav/test").asString().body).isEqualTo("COPY")
    }
}
