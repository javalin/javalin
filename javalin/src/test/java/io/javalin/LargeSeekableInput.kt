package io.javalin

import java.io.InputStream

class LargeSeekableInput(private val prefixSize: Long, private val contentSize: Long) : InputStream() {

    private var alreadyRead = 0L
    private fun remaining(): Long = prefixSize + contentSize - alreadyRead

    override fun available(): Int = when (val rem = remaining()) {
        in 0..Int.MAX_VALUE -> rem.toInt()
        else -> Int.MAX_VALUE
    }

    override fun skip(toSkip: Long): Long = when {
        toSkip <= 0 -> 0
        else -> when (val rem = remaining()) {
            in 0..toSkip -> rem.also { alreadyRead += rem }
            else -> toSkip.also { alreadyRead += toSkip }
        }
    }

    override fun read(): Int = when {
        remaining() == 0L -> -1
        alreadyRead < prefixSize -> ' '.code.also { alreadyRead++ }
        else -> 'J'.code.also { alreadyRead++ }
    }

}
