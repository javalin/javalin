package io.javalin.http

import io.javalin.testing.HttpUtil
import org.assertj.core.api.Assertions.assertThat
import io.javalin.testing.TestUtil
import org.junit.jupiter.api.Test

// Note: Heavy W.I.P by a person that doesn't know Kotlin or Tests.
class ResponseExceptionTest {

    @Test
    fun `response types`() {
        val responses = arrayOf(
            Triple("TemporaryRedirectResponse", HttpCode.TEMPORARY_REDIRECT.status, Pair(HttpCode.TEMPORARY_REDIRECT.message, mapOf(Pair("key", "value")))),
            Triple("PermanentRedirectResponse", HttpCode.PERMANENT_REDIRECT.status, Pair(HttpCode.PERMANENT_REDIRECT.message, mapOf(Pair("key", "value")))),
            Triple("FoundRedirectResponse", HttpCode.FOUND.status, Pair(HttpCode.FOUND.message, mapOf(Pair("key", "value")))),
            Triple("BadRequestResponse", HttpCode.BAD_REQUEST.status, Pair(HttpCode.BAD_REQUEST.message, mapOf(Pair("key", "value")))),
            Triple("UnauthorizedResponse", HttpCode.UNAUTHORIZED.status, Pair(HttpCode.UNAUTHORIZED.message, mapOf(Pair("key", "value")))),
            Triple("ForbiddenResponse", HttpCode.FORBIDDEN.status, Pair(HttpCode.FORBIDDEN.message, mapOf(Pair("key", "value")))),
            Triple("NotFoundResponse", HttpCode.NOT_FOUND.status, Pair(HttpCode.NOT_FOUND.message, mapOf(Pair("key", "value")))),
            Triple("MethodNotAllowedResponse", HttpCode.METHOD_NOT_ALLOWED.status, Pair(HttpCode.METHOD_NOT_ALLOWED.message, mapOf(Pair("key", "value")))),
            Triple("ConflictResponse", HttpCode.CONFLICT.status, Pair(HttpCode.CONFLICT.message, mapOf(Pair("key", "value")))),
            Triple("InternalServerErrorResponse", HttpCode.INTERNAL_SERVER_ERROR.status, Pair(HttpCode.INTERNAL_SERVER_ERROR.message, mapOf(Pair("key", "value")))),
            Triple("BadGatewayResponse", HttpCode.BAD_GATEWAY.status, Pair(HttpCode.BAD_GATEWAY.message, mapOf(Pair("key", "value")))),
            Triple("ServiceUnavailableResponse", HttpCode.SERVICE_UNAVAILABLE.status, Pair(HttpCode.SERVICE_UNAVAILABLE.message, mapOf(Pair("key", "value")))),
            Triple("GatewayTimeoutResponse", HttpCode.GATEWAY_TIMEOUT.status, Pair(HttpCode.GATEWAY_TIMEOUT.message, mapOf(Pair("key", "value")))),
        )

        TestUtil.test { app, http ->
            for (response: Triple<String, Int, Pair<String, Map<String, String>>> in responses) {
                val responseType = response.first;
                val statusCode = response.second;
                val message = response.third.first;
                val details = response.third.second;
                val clazz = Class.forName("io.javalin.http." + responseType);

                app.post("/${responseType}/") {
                    if (RedirectResponse::class.java.isAssignableFrom(clazz)) {
                        val constructor = clazz.getConstructor(String::class.java, String::class.java, Map::class.java)
                        throw constructor.newInstance("www.google.com", message, details) as RedirectResponse;
                    } else {
                        val constructor = clazz.getConstructor(String::class.java, Map::class.java)
                        throw constructor.newInstance(message, details) as HttpResponseException;
                    }
                }

                this.run(http, clazz, statusCode, message, details)
            }
        }
    }

    fun run(
        http: HttpUtil,
        clazz: Class<*>, statusCode: Int, message: String, details: Map<String, String>
    ) {
        val httpResponse = http.post("/${clazz.simpleName}/").asString()

        assertThat(httpResponse.status).isEqualTo(statusCode)
        // TODO: Deserialize body contents and assert the message & data
    }

}
