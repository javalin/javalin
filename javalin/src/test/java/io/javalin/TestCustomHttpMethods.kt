/*
 * Javalin - https://javalin.io
 * Copyright 2024 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.HttpStatus
import io.javalin.testing.TestUtil
import kong.unirest.core.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests for custom HTTP methods like WebDAV methods (PROPFIND, MKCOL, etc)
 */
class TestCustomHttpMethods {

    @Test
    fun `custom HTTP method PROPFIND works`() = TestUtil.test { app, http ->
        app.addHttpHandler("PROPFIND", "/dav/*") { ctx -> 
            ctx.result("PROPFIND response")
        }
        
        val response = Unirest.request("PROPFIND", http.origin + "/dav/folder").asString()
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(response.body).isEqualTo("PROPFIND response")
    }

    @Test
    fun `custom HTTP method MKCOL works`() = TestUtil.test { app, http ->
        app.addHttpHandler("MKCOL", "/dav/*") { ctx ->
            ctx.result("MKCOL response").status(HttpStatus.CREATED)
        }
        
        val response = Unirest.request("MKCOL", http.origin + "/dav/newcollection").asString()
        assertThat(response.status).isEqualTo(HttpStatus.CREATED.code)
        assertThat(response.body).isEqualTo("MKCOL response")
    }

    @Test
    fun `custom HTTP method with unknown method returns 404`() = TestUtil.test { app, http ->
        app.addHttpHandler("PROPFIND", "/dav/*") { ctx ->
            ctx.result("PROPFIND response")
        }
        
        val response = Unirest.request("CUSTOMMETHOD", http.origin + "/dav/folder").asString()
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND.code)
    }

    @Test
    fun `custom HTTP method with path parameter works`() = TestUtil.test { app, http ->
        app.addHttpHandler("PROPFIND", "/dav/{path}") { ctx ->
            ctx.result("Path: " + ctx.pathParam("path"))
        }
        
        val response = Unirest.request("PROPFIND", http.origin + "/dav/myfile.txt").asString()
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(response.body).isEqualTo("Path: myfile.txt")
    }

    @Test
    fun `Context method returns raw method for custom HTTP methods`() = TestUtil.test { app, http ->
        var capturedMethod = ""
        app.addHttpHandler("PROPFIND", "/test") { ctx ->
            capturedMethod = ctx.req().method
            ctx.result("OK")
        }
        
        Unirest.request("PROPFIND", http.origin + "/test").asString()
        assertThat(capturedMethod).isEqualTo("PROPFIND")
    }
}
