package io.javalin.plugin.graphql.helpers

import io.javalin.plugin.graphql.graphql.SubscriptionGraphql
import reactor.core.publisher.Flux
import java.time.Duration
import kotlin.random.Random

class SubscriptionExample: SubscriptionGraphql {
    fun counter(): Flux<Int> = Flux.interval(Duration.ofMillis(100)).map { 1 }
}
