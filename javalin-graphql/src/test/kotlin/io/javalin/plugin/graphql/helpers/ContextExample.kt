package io.javalin.plugin.graphql.helpers

class ContextExample {
    var hi = "Hi"
    val hello = "Hello World"

    fun updateHi(text: String) {
        this.hi = text
    }
}
