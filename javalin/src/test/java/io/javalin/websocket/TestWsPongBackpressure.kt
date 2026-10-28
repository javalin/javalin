package io.javalin.websocket

import io.javalin.Javalin
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.java_websocket.framing.PingFrame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

class TestWsPongBackpressure {

    @Test
    @Timeout(15)
    fun `websocket stops processing incoming pings when outgoing pongs are queued`() {
        val pingCount = 10
        val blockingFrameSize = 10 * 1024 * 1024

        val serverPingsReceived = AtomicInteger()
        val serverFirstPingReceived = CompletableFuture<Boolean>()
        val serverSecondPingReceived = CompletableFuture<Boolean>()

        val clientReaderPaused = CompletableFuture<Void>()
        val clientReaderResume = CompletableFuture<Void>()
        val clientReceivedAllPongs = CountDownLatch(pingCount)

        val app = Javalin.create { config ->
            config.routes.ws("/ws") { ws ->
                ws.onConnect { ctx ->
                    // The client blocks reading when the first message arrives.
                    ctx.send("client stop reading")
                    // Send a second large frame to fill up the TCP output buffer.
                    // Every output after that ones gets queued in the application.
                    ctx.send(ByteBuffer.allocate(blockingFrameSize))
                }
                ws.onPing {
                    when (serverPingsReceived.incrementAndGet()) {
                        1 -> serverFirstPingReceived.complete(true)
                        2 -> serverSecondPingReceived.complete(true)
                    }
                }
            }
        }

        TestUtil.test(app) { _, _ ->
            val client = WsTestClient(
                app, "/ws",
                onMessage = {
                    // signal to the test that reading is blocked now
                    clientReaderPaused.complete(null);
                    // block the reader thread until the signal to resume reading
                    clientReaderResume.get(10, TimeUnit.SECONDS);
                },
                onPong = { clientReceivedAllPongs.countDown() }
            )

            try {
                client.connectBlocking()
                // wait until the client reader is blocked
                clientReaderPaused.get(1, TimeUnit.SECONDS)

                // ping the server repeatedly while the client doesn't read
                repeat(pingCount) {
                    client.sendFrame(PingFrame())
                }

                // verify that only the first ping gets processed on the server
                assertThat(serverFirstPingReceived.get(1, TimeUnit.SECONDS)).isTrue()
                assertThatThrownBy({ serverSecondPingReceived.get(100, TimeUnit.MILLISECONDS) })
                    .isInstanceOf(TimeoutException::class.java)

                // resume reading on the client and verify that all queued pings get processed
                clientReaderResume.complete(null)
                assertThat(clientReceivedAllPongs.await(1, TimeUnit.SECONDS)).isTrue()
            } finally {
                clientReaderResume.complete(null)
                client.disconnectBlocking()
            }
        }
    }
}
