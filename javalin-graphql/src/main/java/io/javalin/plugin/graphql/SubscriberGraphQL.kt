package io.javalin.plugin.graphql

import graphql.ExecutionResult
import io.javalin.plugin.json.JavalinJson
import io.javalin.websocket.WsMessageContext
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

class SubscriberGraphQL(private val ctx: WsMessageContext) : Subscriber<ExecutionResult> {
    private val subscriptionRef = AtomicReference<Subscription>()

    override fun onError(error: Throwable) {
        throw error
    }

    override fun onSubscribe(s: Subscription?) {
        subscriptionRef.set(s)
        request(1)
    }

    override fun onComplete() {
    }

    override fun onNext(result: ExecutionResult) {
        try {
            val data = result.getData<Any>()
            ctx.send(JavalinJson.toJson(data))
        } catch (e: IOException) {
            e.printStackTrace()
        }
        request(1)
    }

    private fun request(n: Int) {
        val subscription = subscriptionRef.get()
        subscription?.request(n.toLong())
    }
}
