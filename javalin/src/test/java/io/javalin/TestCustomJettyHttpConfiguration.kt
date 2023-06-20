/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.ForwardedRequestCustomizer
import org.eclipse.jetty.server.HttpConnectionFactory
import org.junit.jupiter.api.Test

class TestCustomJettyHttpConfiguration {
    private val customizer = ForwardedRequestCustomizer()

    @Test
    fun `customizers get added`() = TestUtil.test(Javalin.create { cfg ->
        cfg.jetty.modifyHttpConfiguration() {
            it.customizers.add(customizer)
        }
    }) { javalin, http ->
        javalin.jettyServer.server().connectors.forEach {
            val cf = it.getConnectionFactory(HttpConnectionFactory::class.java)
            assertThat(cf.httpConfiguration.customizers).contains(customizer)
        }
    }

    @Test
    fun `X-Fowarded-Proto Works With Customizer`() = TestUtil.test(Javalin.create { cfg ->
        cfg.jetty.modifyHttpConfiguration {
            it.customizers.add(customizer)
        }
    }) { javalin, http ->
        javalin.get("/") {
            it.result(it.scheme())
        }

        val response = http.get("/", mapOf("X-Forwarded-Proto" to "https")).body

        assertThat(response).isEqualTo("https")
    }

    @Test
    fun `X-Fowarded-Proto Does Not Work Without Customizer`() = TestUtil.test(Javalin.create()) { javalin, http ->
        javalin.get("/") {
            it.result(it.scheme())
        }

        val response = http.get("/", mapOf("X-Forwarded-Proto" to "https")).body

        assertThat(response).isNotEqualTo("https")
    }
}
