package io.javalin.testing

import io.javalin.http.HttpStatus
import io.javalin.testtools.HttpClient
import io.javalin.testtools.Request
import io.javalin.testtools.Response
import java.util.function.Consumer

// Extension functions to make migration from old test utilities easier

fun HttpClient.get(path: String, headers: Map<String, String>): Response {
    return this.get(path) { builder ->
        headers.forEach { (name, value) ->
            builder.header(name, value)
        }
    }
}

fun Response.header(name: String): String {
    return this.headers().get(name)?.firstOrNull() ?: ""
}

val Response.status: Int
    get() = this.code()

val Response.body: String
    get() = this.body.string()

fun Response.httpCode(): HttpStatus {
    return HttpStatus.forStatus(this.code())
}

// For compatibility with old test code that calls .asString() on responses
fun Response.asString(): Response = this

fun HttpClient.getBody(path: String): String {
    return this.getFollowingRedirects(path).body.string()
}

fun HttpClient.getBody(path: String, headers: Map<String, String>): String {
    return this.get(path, headers).body.string()
}

// Helper to follow redirects (up to 10 redirects)
private fun HttpClient.getFollowingRedirects(path: String, maxRedirects: Int = 10): Response {
    var currentPath = path
    var redirectCount = 0

    while (redirectCount < maxRedirects) {
        val response = this.get(currentPath)
        val statusCode = response.code()

        // Check if it's a redirect status code
        if (statusCode in 300..399) {
            val location = response.headers().get("Location")?.firstOrNull()
            if (location != null) {
                // Handle relative vs absolute URLs
                currentPath = if (location.startsWith("http")) {
                    // Absolute URL - extract the path
                    java.net.URI(location).path + (java.net.URI(location).query?.let { "?$it" } ?: "")
                } else {
                    location
                }
                redirectCount++
                continue
            }
        }

        // Not a redirect or no location header, return the response
        return response
    }

    // Too many redirects
    throw IllegalStateException("Too many redirects (>$maxRedirects)")
}

fun HttpClient.call(method: String, path: String): Response {
    return this.request(path) { builder ->
        when (method.uppercase()) {
            "GET" -> builder.get()
            "POST" -> builder.post(java.net.http.HttpRequest.BodyPublishers.noBody())
            "PUT" -> builder.put(java.net.http.HttpRequest.BodyPublishers.noBody())
            "DELETE" -> builder.delete()
            "PATCH" -> builder.patch(java.net.http.HttpRequest.BodyPublishers.noBody())
            else -> builder.get()
        }
    }
}

fun HttpClient.htmlGet(path: String): Response {
    return this.get(path) { it.header("Accept", "text/html") }
}

fun HttpClient.jsonGet(path: String): Response {
    return this.get(path) { it.header("Accept", "application/json") }
}

fun HttpClient.options(path: String): Response {
    return this.options(path, emptyMap())
}

fun HttpClient.options(path: String, headers: Map<String, String>): Response {
    // Build the full URL
    val url = origin + path

    // Create an OPTIONS request using Java's HttpClient
    val requestBuilder = java.net.http.HttpRequest.newBuilder()
        .uri(java.net.URI.create(url))
        .method("OPTIONS", java.net.http.HttpRequest.BodyPublishers.noBody())

    // Add headers
    headers.forEach { (name, value) ->
        requestBuilder.header(name, value)
    }

    val httpRequest = requestBuilder.build()

    // Use a new HttpClient with the same configuration as the test client
    // We need to use the same cookie handler to maintain session state
    val jdkClient = java.net.http.HttpClient.newBuilder()
        .cookieHandler(java.net.CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL))
        .build()

    val response = jdkClient.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString())

    // Wrap in our Response type
    return io.javalin.testtools.Response(response)
}

