/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.testing.HttpUtil
import io.javalin.testing.TestUtil
import jakarta.servlet.DispatcherType
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.FilterConfig
import jakarta.servlet.ServletContextEvent
import jakarta.servlet.ServletContextListener
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.ee10.servlet.FilterHolder
import org.junit.jupiter.api.Test
import java.util.*

class TestConfigureServletContextHandler {

    @Test
    fun `adding an event listener to the ServletContextHandler works`() = TestUtil.runLogLess {
        val listener = object : ServletContextListener {
            var called = false
            override fun contextInitialized(ev: ServletContextEvent?) {
                called = true
            }

            override fun contextDestroyed(ev: ServletContextEvent?) {
                called = true
            }
        }

        val app = Javalin.create {
            it.jetty.modifyServletContextHandler { handler ->
                handler.addEventListener(listener)
            }
        }.start(0)

        val http = HttpUtil(app.port())
        http.htmlGet("/")

        assertThat(listener.called).isTrue()
    }

    @Test
    fun `adding a filter to the ServletContextHandler works`() {
        val filter = object : Filter {
            var initialized = false
            var called = false
            var destroyed = false
            override fun init(config: FilterConfig?) {
                initialized = true
            }

            override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
                called = true
                chain?.doFilter(request, response)
            }

            override fun destroy() {
                destroyed = true
            }
        }

        val filterJavalin = Javalin.create {
            it.jetty.modifyServletContextHandler { handler ->
                handler.addFilter(FilterHolder(filter), "/*", EnumSet.allOf(DispatcherType::class.java))
            }
        }

        TestUtil.test(filterJavalin) { app, http ->
            app.get("/test") { it.result("Test") }
            val response = http.get("/test")
            assertThat(response.body).isEqualTo("Test")
            assertThat(filter.initialized).isTrue()
            assertThat(filter.called).isTrue()
        }

    }

}
