/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver

interface EmbeddedServer {

    @Throws(Exception::class)
    fun start(host: String, port: Int): Int

    @Throws(InterruptedException::class)
    fun join()

    fun stop()
    fun activeThreadCount(): Int
    fun attribute(key: String): Any
}
