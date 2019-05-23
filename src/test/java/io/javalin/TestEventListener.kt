/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.misc.HttpUtil
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener

class TestEventListener {

    @Test
    fun `Adding a listener works and listener gets called`() {
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
            it.addListener(listener)
        }.start()

        val http = HttpUtil(app)
        http.htmlGet("/");

        assertTrue(listener.called)
    }

}
