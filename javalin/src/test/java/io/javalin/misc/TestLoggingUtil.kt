package io.javalin.misc

import java.io.ByteArrayOutputStream
import java.io.PrintStream

fun captureStdOut(run: () -> Unit): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    val printStream = PrintStream(byteArrayOutputStream)
    val oldOut = System.out
    val oldErr = System.err
    System.setOut(printStream)
    System.setErr(printStream)

    try {
        run()
    } finally {
        System.out.flush()
        System.setOut(oldOut)
        System.setErr(oldErr)
    }

    return byteArrayOutputStream.toString()
}
