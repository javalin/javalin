package io.javalin.plugin.graphql

import com.mashape.unirest.http.JsonNode
import io.javalin.Javalin
import io.javalin.plugin.graphql.helpers.*
import io.javalin.testing.HttpUtil
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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
    fun multiQuery() = TestUtil.test(shortTimeoutServer()) { server, httpUtil ->
        val queries = "query X { hello } query Y { echo(message: \\\"$newMessage\\\") }"

        val jsonX = sendPetition(httpUtil, "{\"query\": \"$queries\", \"operationName\": \"X\"}")
        assertEquals(jsonX.getJSONObject("data").getString("hello"), message)
        assertFalse(jsonX.getJSONObject("data").has("echo"))

        val jsonY = sendPetition(httpUtil, "{\"query\": \"$queries\", \"operationName\": \"Y\"}")
        assertFalse(jsonY.getJSONObject("data").has("hello"))
        assertEquals(jsonY.getJSONObject("data").getString("echo"), newMessage)
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
    fun contextWithoutAuthorized() = TestUtil.test(shortTimeoutServer()) { server, httpUtil ->
        val json = sendPetition(httpUtil, "{\"query\": \"{ isAuthorized }\"}")
        assertFalse(json.getJSONObject("data").getBoolean("isAuthorized"))
    }

    @Test
    fun contextWithAuthorized() = TestUtil.test(shortTimeoutServer()) { server, httpUtil ->
        val json = sendPetition(httpUtil, "Beare token","{\"query\": \"{ isAuthorized }\"}")
        assertTrue(json.getJSONObject("data").getBoolean("isAuthorized"))
    }

    @Test
    fun subscribe() = TestUtil.test(shortTimeoutServer()) { server, httpUtil ->
        val testClient1_1 = TestClient(server, graphqlPath)
        doAndSleepWhile({ testClient1_1.connect() }, { !testClient1_1.isOpen })
        doAndSleep { testClient1_1.send("{\"query\": \"subscription { counter }\"}") }
        doAndSleepWhile({ testClient1_1.close() }, { testClient1_1.isClosing })
        assertThat(server.logger().log).containsAnyOf("{\"counter\":1}")
    }

    @Test
    fun subscribeWithoutContext() = TestUtil.test(shortTimeoutServer()) { server, httpUtil ->
        val testClient1_1 = TestClient(server, graphqlPath)
        doAndSleepWhile({ testClient1_1.connect() }, { !testClient1_1.isOpen })
        doAndSleep { testClient1_1.send("{\"query\": \"subscription { counterUser }\"}") }
        doAndSleepWhile({ testClient1_1.close() }, { testClient1_1.isClosing })
        assertThat(server.logger().log).containsAnyOf("{\"counterUser\":\"${SubscriptionExample.anonymous_message} ~> 1\"}")
    }

    @Test
    fun subscribeWithContext() = TestUtil.test(shortTimeoutServer()) { server, httpUtil ->
        val testClient1_1 = TestClient(server, graphqlPath)
        val tokenUser = "token"
        testClient1_1.addHeader("Authorization", "Beare $tokenUser")
        doAndSleepWhile({ testClient1_1.connect() }, { !testClient1_1.isOpen })
        doAndSleep { testClient1_1.send("{\"query\": \"subscription { counterUser }\"}") }
        doAndSleepWhile({ testClient1_1.close() }, { testClient1_1.isClosing })
        assertThat(server.logger().log).containsAnyOf("{\"counterUser\":\"$tokenUser ~> 1\"}")
    }

    private fun sendPetition(httpUtil: HttpUtil, authorization: String, body: String): JSONObject {
        val response = httpUtil.post(graphqlPath).header("Authorization", authorization).body(JsonNode(body)).asString()
        return JSONObject(response.body)
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
        if (this.attribute<TestLogger>(TestLogger::class.java.name) == null) {
            this.attribute(TestLogger::class.java.name, TestLogger(ArrayList()))
        }
        return this.attribute(TestLogger::class.java.name)
    }


    private fun shortTimeoutServer(): Javalin {
        return Javalin.create {
            val graphQLPluginBuilder = GraphQLPluginBuilder(graphqlPath, ContextFactoryExample(), ContextWsFactoryExample())
                .add("io.javalin.plugin.graphql")
                .add("io.javalin.plugin.graphql.helpers")
                .register(QueryExample(message))
                .register(MutationExample(message))
                .register(SubscriptionExample())

            it.registerPlugin(graphQLPluginBuilder.build())
        }

    }

    private fun doAndSleep(func: () -> Unit) = func.invoke().also { Thread.sleep(1000) }

}
