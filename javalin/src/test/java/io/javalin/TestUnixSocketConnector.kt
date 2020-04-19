package io.javalin

import io.javalin.testing.TestUtil
import jnr.unixsocket.UnixSocketAddress
import jnr.unixsocket.UnixSocketChannel
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.unixsocket.UnixSocketConnector
import org.junit.Test
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class TestUnixSocketConnector {

    @Test
    fun `using unixsocket`() {

        if (System.getProperty("os.name").contains("windows", ignoreCase = true)) {
            return // this test can never succeed on windows
        }

        val socketFileName = "/tmp/javalin.sock"
        val testPath = "/unixsocket"
        val expectedResultString = "hello unixsocket"

        val unixSocketJavalin = Javalin.create {
            it.server {
                val server = Server()
                val serverConnector = ServerConnector(server)
                serverConnector.port = 0
                server.addConnector(serverConnector)
                val unixSocketConnector = UnixSocketConnector(server)
                unixSocketConnector.unixSocket = socketFileName
                server.addConnector(unixSocketConnector)
                server
            }
        }

        unixSocketJavalin.get(testPath) { ctx -> ctx.status(200).result(expectedResultString) }

        TestUtil.test(unixSocketJavalin) { _, _ ->

            val socketAddress = UnixSocketAddress(File(socketFileName))
            val socket = UnixSocketChannel.open(socketAddress).socket()
            val w = BufferedWriter(OutputStreamWriter(socket.outputStream, "UTF8"))
            val r = BufferedReader(InputStreamReader(socket.inputStream))
            val response = arrayListOf<String>()

            w.write("GET $testPath HTTP/1.0\r\nHost:localhost\r\n\r\n")
            w.flush()

            while (true) {
                val line = r.readLine() ?: break
                response.add(line)
            }

            w.close()
            r.close()

            val arr = response.first().split(" ")
            assertThat("${arr.get(1)} ${arr.last()}").isEqualTo("200 OK")
            assertThat(response.last()).isEqualTo(expectedResultString)
        }

    }

}
