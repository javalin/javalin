package io.javalin

import io.javalin.http.HttpStatus.OK
import io.javalin.testing.TestEnvironment
import io.javalin.testing.TestUtil

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.unixdomain.server.UnixDomainServerConnector
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import kotlin.io.path.Path


class TestUnixSocketConnector {

    @Test
    fun `using unixsocket`() {
        assumeTrue(TestEnvironment.isNotWindows) // this test can never succeed on windows
        assumeTrue(Runtime.version().feature() >= 16) // the jetty-unixdomain-server module requires java 16
        val socketFileName = "/tmp/javalin.sock"
        val testPath = "/unixsocket"
        val expectedResultString = "hello unixsocket"

        val unixSocketJavalin = Javalin.create {
            it.jetty.addConnector { server, _ ->
                val serverConnector = ServerConnector(server)
                serverConnector.port = 0
                serverConnector
            }
            it.jetty.addConnector { server, _ ->
                val unixSocketConnector = UnixDomainServerConnector(server)
                unixSocketConnector.unixDomainPath = Path(socketFileName)
                unixSocketConnector
            }
        }

        unixSocketJavalin.unsafe.routes.get(testPath) { it.status(OK).result(expectedResultString) }

        TestUtil.test(unixSocketJavalin) { _, _ ->

            val of = Class.forName("java.net.UnixDomainSocketAddress").getMethod("of", socketFileName.javaClass)
            val socketAddress = of.invoke(Any(), socketFileName)
            val socketChannel = SocketChannel.open(socketAddress as SocketAddress)

            val message = "GET $testPath HTTP/1.0\r\nHost:localhost\r\n\r\n"
            val messageBuffer = ByteBuffer.allocate(1024)
            messageBuffer.clear()
            messageBuffer.put(message.toByteArray(Charsets.UTF_8))
            messageBuffer.flip()

            while (messageBuffer.hasRemaining()) {
                socketChannel.write(messageBuffer);
            }

            val responseBuffer = ByteBuffer.allocate(1024)
            socketChannel.read(responseBuffer)
            responseBuffer.flip()
            val response = String(responseBuffer.array()).split("\n")
            socketChannel.close()

            val arr = response.first().split(" ")
            assertThat("${arr.get(1)} ${arr.last()}").contains("200 OK")
            assertThat(response.last()).contains(expectedResultString)
        }

    }

}
