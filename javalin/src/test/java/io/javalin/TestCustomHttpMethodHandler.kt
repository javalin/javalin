/*
 * Javalin - https://javalin.io
 * Copyright 2024 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.Handler
import io.javalin.http.HttpStatus
import io.javalin.http.util.handleCustomMethod
import io.javalin.testing.TestUtil
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests for CustomHttpMethodHandler utility
 */
class TestCustomHttpMethodHandler {

    @Test
    fun `extension function handles PROPFIND method`() = TestUtil.test { app, http ->
        app.before("/dav/*") { ctx ->
            ctx.handleCustomMethod(
                "PROPFIND" to Handler { it.result("PROPFIND response") },
                "MKCOL" to Handler { it.result("MKCOL response").status(HttpStatus.CREATED) }
            )
        }
        
        val response = Unirest.request("PROPFIND", http.origin + "/dav/folder").asString()
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(response.body).isEqualTo("PROPFIND response")
    }

    @Test
    fun `extension function handles MKCOL method`() = TestUtil.test { app, http ->
        app.before("/dav/*") { ctx ->
            ctx.handleCustomMethod(
                "PROPFIND" to Handler { it.result("PROPFIND response") },
                "MKCOL" to Handler { it.result("MKCOL response").status(HttpStatus.CREATED) }
            )
        }
        
        val response = Unirest.request("MKCOL", http.origin + "/dav/newcollection").asString()
        assertThat(response.status).isEqualTo(HttpStatus.CREATED.code)
        assertThat(response.body).isEqualTo("MKCOL response")
    }

    @Test
    fun `unhandled custom method returns 404`() = TestUtil.test { app, http ->
        app.before("/dav/*") { ctx ->
            ctx.handleCustomMethod(
                "PROPFIND" to Handler { it.result("PROPFIND response") }
            )
        }
        
        val response = Unirest.request("UNKNOWNMETHOD", http.origin + "/dav/folder").asString()
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND.code)
    }

    @Test
    fun `can access path parameters in custom method handler`() = TestUtil.test { app, http ->
        app.before("/dav/{path}") { ctx ->
            ctx.handleCustomMethod(
                "PROPFIND" to Handler { it -> it.result("Path: " + it.pathParam("path")) }
            )
        }
        
        val response = Unirest.request("PROPFIND", http.origin + "/dav/myfile.txt").asString()
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(response.body).isEqualTo("Path: myfile.txt")
    }

    @Test
    fun `method names are case insensitive`() = TestUtil.test { app, http ->
        app.before("/dav/*") { ctx ->
            ctx.handleCustomMethod(
                "propfind" to Handler { it -> it.result("PROPFIND response") }  // lowercase in registration
            )
        }
        
        val response = Unirest.request("PROPFIND", http.origin + "/dav/folder").asString()  // uppercase in request
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(response.body).isEqualTo("PROPFIND response")
    }

    @Test
    fun `can use standard HTTP methods with custom handler`() = TestUtil.test { app, http ->
        // This demonstrates that the utility can be used for standard methods too, though not recommended
        app.before("/test/*") { ctx ->
            ctx.handleCustomMethod(
                "GET" to Handler { it -> it.result("Custom GET response") }
            )
        }
        
        val response = http.get("/test/path")
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(response.body).isEqualTo("Custom GET response")
    }
}
