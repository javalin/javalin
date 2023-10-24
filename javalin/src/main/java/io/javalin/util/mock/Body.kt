package io.javalin.util.mock

import java.io.InputStream

fun interface Body {
    fun toInputStream(): InputStream
}

class StringBody(val body: String) : Body {
    override fun toInputStream(): InputStream = body.byteInputStream()
}
