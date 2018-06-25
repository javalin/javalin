package io.javalin.serversentevent

interface Emitter {
    fun event(event: String, data: String)
    fun isClose(): Boolean
}