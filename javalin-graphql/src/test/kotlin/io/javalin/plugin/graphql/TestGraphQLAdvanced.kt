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


class TestGraphQLAdvanced {

    private val graphqlPath = "/graphql"
    private val message = "Hello World"


    @Test
    fun runMiddleWare() = TestUtil.test(shortTimeoutServer()) { server, httpUtil ->
        val response = httpUtil.post(graphqlPath)
                .header("test", message)
                .body(JsonNode("{\"query\": \"{ context { hi} }\"}")).asString()
        val json = JSONObject(response.body)
        assertEquals(json.getJSONObject("data").getJSONObject("context").getString("hi"), message)
    }

    private fun shortTimeoutServer(): Javalin {
        val context = ContextExample()
        return Javalin.create {
            val graphQLOption = GraphQLOptions(graphqlPath, context)
                    .addPackage("io.javalin.plugin.graphql")
                    .addPackage("io.javalin.plugin.graphql.helpers")
                    .register(QueryExample(message))
                    .register(MutationExample(message))
                    .register(SubscriptionExample())
                    .setMiddleHandler { ctx ->  context.updateHi(ctx.header("test")!!)}
            it.registerPlugin(GraphQLPlugin(graphQLOption))
        }
                .after { ctx -> ctx.req.asyncContext.timeout = 10 }
    }

    private fun doAndSleep(func: () -> Unit) = func.invoke().also { Thread.sleep(1000) }

}
