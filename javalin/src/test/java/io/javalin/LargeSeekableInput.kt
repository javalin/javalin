package io.javalin

import java.io.InputStream

class LargeSeekableInput(private val prefixSize: Long, private val contentSize: Long): InputStream() {
    private var alreadyRead = 0L

    private fun remaining(): Long = prefixSize + contentSize - alreadyRead

    override fun available(): Int {
        val rem = remaining()
        return if (rem < Int.MAX_VALUE) {
            rem.toInt()
        } else {
            Int.MAX_VALUE
        }
    }

    override fun skip(toSkip: Long): Long {
        if (toSkip <= 0) {
            return 0;
        }
        val rem = remaining()
        return if (rem < toSkip) {
            alreadyRead += rem
            rem
        } else {
            alreadyRead += toSkip
            toSkip
        }
    }

    override fun read(): Int {
        return if (remaining() == 0L) {
            -1
        } else if (alreadyRead < prefixSize) {
            alreadyRead++
            ' '.code
        } else {
            alreadyRead++
            'J'.code
        }
    }
}
