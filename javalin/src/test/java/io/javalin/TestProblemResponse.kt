/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.http.ContentType
import io.javalin.http.ProblemResponse
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import java.net.URI

class TestProblemResponse {

    private val objectMapper = ObjectMapper()

    @Test
    fun `problem response writes standard and extension members`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/") {
            it.problem(
                ProblemResponse(
                    title = "Validation failed",
                    type = URI.create("https://example.com/problems/validation"),
                    status = 422,
                    detail = "One or more fields are invalid.",
                    instance = URI.create("/requests/123"),
                    additionalMembers = mapOf("errors" to listOf(mapOf("field" to "name"))),
                )
            )
        }

        val response = http.get("/")
        val body = objectMapper.readTree(response.body)

        assertThat(response.status).isEqualTo(422)
        assertThat(response.headers.getFirst("Content-Type")).startsWith(ContentType.PROBLEM_JSON)
        assertThat(body["type"].asText()).isEqualTo("https://example.com/problems/validation")
        assertThat(body["title"].asText()).isEqualTo("Validation failed")
        assertThat(body["status"].asInt()).isEqualTo(422)
        assertThat(body["detail"].asText()).isEqualTo("One or more fields are invalid.")
        assertThat(body["instance"].asText()).isEqualTo("/requests/123")
        assertThat(body["errors"][0]["field"].asText()).isEqualTo("name")
    }

    @Test
    fun `omitted optional members do not change the current status`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/") { it.status(418).problem(ProblemResponse(title = "Teapot")) }

        val response = http.get("/")
        val body = objectMapper.readTree(response.body)

        assertThat(response.status).isEqualTo(418)
        assertThat(body["type"].asText()).isEqualTo("about:blank")
        assertThat(body["status"]).isNull()
        assertThat(body["detail"]).isNull()
        assertThat(body["instance"]).isNull()
    }

    @Test
    fun `extension members cannot override standard members`() {
        assertThatIllegalArgumentException().isThrownBy {
            ProblemResponse(title = "Failure", additionalMembers = mapOf("title" to "replacement"))
        }
    }
}
