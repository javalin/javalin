package io.javalin.plugin.graphql

import com.mashape.unirest.http.JsonNode
import io.javalin.Javalin
import io.javalin.plugin.graphql.helpers.ContextExample
import io.javalin.plugin.graphql.helpers.MutationExample
import io.javalin.plugin.graphql.helpers.QueryExample
import io.javalin.plugin.graphql.helpers.SubscriptionExample
import io.javalin.testing.HttpUtil
import io.javalin.testing.TestUtil
import junit.framework.Assert.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import org.junit.Test
import java.net.URI
import java.time.Duration
import java.util.concurrent.TimeoutException


class TestGraphQL {

    private val graphqlPath = "/graphql"
    private val message = "Hello World"
    private val newMessage = "hi"

    data class TestLogger(val log: ArrayList<String>)

    @Test
    fun query() = TestUtil.test(shortTimeoutServer()) { server, httpUtil ->
        val json = sendPetition(httpUtil, "{\"query\": \"{ hello }\"}")
        assertEquals(json.getJSONObject("data").getString("hello"), message)
    }

    @Test
    fun mutation() = TestUtil.test(shortTimeoutServer()) { server, httpUtil ->
        val mutation = "mutation { changeMessage(newMessage: \\\"$newMessage\\\") }"
        val json = sendPetition(httpUtil,  "{\"query\": \"$mutation\"}")
        assertEquals(json.getJSONObject("data").getString("changeMessage"), newMessage)
    }

    @Test
    fun mutation_with_variables() = TestUtil.test(shortTimeoutServer()) { server, httpUtil ->
        val mutation = "mutation changeMessage(\$message: String!){changeMessage(newMessage: \$message)}"
        val variables = "{\"message\": \"$newMessage\"}"
        val body = "{\"variables\": $variables, \"query\": \"$mutation\" }"
        val json = sendPetition(httpUtil, body)
        assertEquals(json.getJSONObject("data").getString("changeMessage"), newMessage)
    }

    @Test
    fun context() = TestUtil.test(shortTimeoutServer()) { server, httpUtil ->
        val json = sendPetition(httpUtil, "{\"query\": \"{ context { hello, hi} }\"}")
        assertEquals(json.getJSONObject("data").getJSONObject("context").getString("hello"), ContextExample().hello)
        assertEquals(json.getJSONObject("data").getJSONObject("context").getString("hi"), ContextExample().hi)
    }

    @Test
    fun subscribe() = TestUtil.test(shortTimeoutServer()) { server, httpUtil ->
        val testClient1_1 = TestClient(server, graphqlPath)

        doAndSleepWhile({ testClient1_1.connect() }, { !testClient1_1.isOpen })
        doAndSleep { testClient1_1.send("{\"query\": \"subscription { counter }\"}") }
        doAndSleepWhile({ testClient1_1.close() }, { testClient1_1.isClosing })
        assertThat(server.logger().log).containsAnyOf("{\"counter\":1}")
    }

    private fun sendPetition(httpUtil: HttpUtil, body: String): JSONObject {
        val response = httpUtil.post(graphqlPath).body(JsonNode(body)).asString()
        val json = JSONObject(response.body)
        return json
    }

    internal open inner class TestClient(var app: Javalin, path: String, headers: Map<String, String> = emptyMap()) : WebSocketClient(URI.create("ws://localhost:" + app.port() + path), Draft_6455(), headers, 0) {

        override fun onOpen(serverHandshake: ServerHandshake) {}
        override fun onClose(i: Int, s: String, b: Boolean) {}
        override fun onError(e: Exception) {}
        override fun onMessage(s: String) {
            app.logger().log.add(s)
        }

        fun connectAndDisconnect() {
            doAndSleepWhile({ connect() }, { !isOpen })
            doAndSleepWhile({ close() }, { !isClosed })
        }
    }

    private fun doAndSleepWhile(slowFunction: () -> Unit, conditionFunction: () -> Boolean, timeout: Duration = Duration.ofSeconds(5)) {
        val startTime = System.currentTimeMillis()
        val limitTime = startTime + timeout.toMillis()

        slowFunction.invoke()

        while (conditionFunction.invoke()) {
            if (System.currentTimeMillis() > limitTime) {
                throw TimeoutException("Wait for condition has timed out")
            }

            Thread.sleep(2)
        }
    }

    private fun Javalin.logger(): TestLogger {
        if (this.attribute(TestLogger::class.java) == null) {
            this.attribute(TestLogger::class.java, TestLogger(ArrayList()))
        }
        return this.attribute(TestLogger::class.java)
    }


    private fun shortTimeoutServer(): Javalin {
        return Javalin.create {
            val graphQLOption = GraphQLOptions(graphqlPath, ContextExample())
                    .addPackage("io.javalin.plugin.graphql")
                    .addPackage("io.javalin.plugin.graphql.helpers")
                    .register(QueryExample(message))
                    .register(MutationExample(message))
                    .register(SubscriptionExample())
            it.registerPlugin(GraphQLPlugin(graphQLOption))
        }
                .after { ctx -> ctx.req.asyncContext.timeout = 10 }
    }

    private fun doAndSleep(func: () -> Unit) = func.invoke().also { Thread.sleep(1000) }

}
