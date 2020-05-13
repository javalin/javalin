package io.javalin.plugin.graphql.helpers

import io.javalin.plugin.graphql.graphql.MutationGraphql

class MutationExample(var message: String) : MutationGraphql {
    fun changeMessage(newMessage: String): String {
        message = newMessage
        return message
    }
}
