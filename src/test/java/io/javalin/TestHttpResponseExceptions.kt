/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.util.Header
import io.javalin.util.TestUtil
import org.eclipse.jetty.http.HttpStatus
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.startsWith
import org.junit.Test

class TestHttpResponseExceptions {

    @Test
    fun `default values work`() = TestUtil.test { app, http ->
        app.get("/") { throw BadRequestResponse() }
        assertThat(http.getBody("/"), `is`("Bad request"))
        assertThat(http.get("/").status, `is`(HttpStatus.BAD_REQUEST_400))
    }

    @Test
    fun `custom message works`() = TestUtil.test { app, http ->
        app.get("/") { throw BadRequestResponse("Really bad request") }
        assertThat(http.getBody("/"), `is`("Really bad request"))
        assertThat(http.get("/").status, `is`(HttpStatus.BAD_REQUEST_400))
    }

    @Test
    fun `response is formatted as text if client wants text`() = TestUtil.test { app, http ->
        app.post("/") { throw ForbiddenResponse() }
        val response = http.post("/").header(Header.ACCEPT, "text/plain").asString()
        assertThat(response.headers.getFirst(Header.CONTENT_TYPE), `is`("text/plain"))
        assertThat(response.status, `is`(HttpStatus.FORBIDDEN_403))
        assertThat(response.body, `is`("Forbidden"))
    }

    @Test
    fun `response is formatted as json if client wants json`() = TestUtil.test { app, http ->
        app.post("/") { throw ForbiddenResponse("Off limits!") }
        val response = http.post("/").header(Header.ACCEPT, "application/json").asString()
        assertThat(response.headers.getFirst(Header.CONTENT_TYPE), `is`("application/json"))
        assertThat(response.status, `is`(HttpStatus.FORBIDDEN_403))
        assertThat(response.body, startsWith("{\"status\": 403, \"message\": \"Off limits!\", \"timestamp\": "))
    }

}
