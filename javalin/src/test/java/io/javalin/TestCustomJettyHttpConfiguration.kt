/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.testing.TestUtil
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.ForwardedRequestCustomizer
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.ServerConnector
import org.junit.jupiter.api.Test

class TestCustomJettyHttpConfiguration {
    private val customizer = ForwardedRequestCustomizer()

    @Test
    fun `customizers get added`() = TestUtil.test(Javalin.create { cfg ->
        cfg.jetty.modifyHttpConfiguration() {
            it.customizers.add(customizer)
        }
    }) { javalin, http ->
        javalin.jettyServer().server().connectors.forEach {
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

    @Test
    fun `custom http configuration is used on custom connectors`() {
        val port = (2000..9999).random()
        val app = Javalin.create { cfg ->
            cfg.jetty.addConnector{ server, httpconf ->
                ServerConnector(server, HttpConnectionFactory(httpconf)).apply {
                    this.port = port
                }
            }
            cfg.jetty.modifyHttpConfiguration {
                it.sendXPoweredBy = true
            }
        }

        TestUtil.test(app) { server, _ ->
            server.get("*") { it.result("PORT WORKS") }
            val response = Unirest.get("http://localhost:$port/").asString()
            assertThat(response.body).isEqualTo("PORT WORKS")
            assertThat(response.headers.getFirst("X-Powered-By")).isNotBlank()

        }

    }

    @Test
    fun `responseBufferSize - default is set to the jetty default for outputBufferSize`() = TestUtil.test { app, http ->
        val firstHttpConnectionFactory = app.jettyServer().server().connectors.firstNotNullOfOrNull { (it as? ServerConnector)?.defaultConnectionFactory as? HttpConnectionFactory }
        assertThat(app.unsafeConfig().http.responseBufferSize)
            .isNotNull
            .isEqualTo(firstHttpConnectionFactory?.httpConfiguration?.outputBufferSize)
    }

    @Test
    fun `responseBufferSize - outputBufferSize set via jetty httpConfiguration is respected`() = TestUtil.test(Javalin.create { config ->
        config.jetty.modifyHttpConfiguration { http -> http.outputBufferSize = 42_007 }
    }) { app, http ->
        assertThat(app.unsafeConfig().http.responseBufferSize)
            .isNotNull
            .isEqualTo(42_007)
    }

    @Test
    fun `responseBufferSize - setting the javalin option has a higher priority than the jetty option`() = TestUtil.test(Javalin.create { config ->
        config.http.responseBufferSize = 777_777
        config.jetty.modifyHttpConfiguration { http -> http.outputBufferSize = 555_555 }
    }) { app, http ->
        assertThat(app.unsafeConfig().http.responseBufferSize)
            .isNotNull
            .isEqualTo(777_777)
    }

}
