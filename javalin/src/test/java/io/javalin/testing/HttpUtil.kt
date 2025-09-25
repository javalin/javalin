/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.testing

import io.javalin.http.ContentType
import io.javalin.http.Header
import io.javalin.http.HttpStatus
import kong.unirest.HttpMethod
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.time.Duration
import java.util.concurrent.CompletableFuture

class HttpUtil(port: Int) {

    @JvmField
    val origin: String = "http://localhost:$port"

    // Use a separate client for redirects to allow toggling
    private var followRedirects = false
    private val cookieManager = CookieManager().apply { setCookiePolicy(CookiePolicy.ACCEPT_ALL) }

    private fun createClient() = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .cookieHandler(cookieManager)
        .followRedirects(if (followRedirects) HttpClient.Redirect.NORMAL else HttpClient.Redirect.NEVER)
        .build()

    fun enableUnirestRedirects() { followRedirects = true }
    fun disableUnirestRedirects() { followRedirects = false }

    // Basic HTTP methods
    fun get(path: String): JavalinHttpResponse = get(path, emptyMap())
    fun get(path: String, headers: Map<String, String>): JavalinHttpResponse {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(origin + path))
            .GET()
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()
        
        val response = createClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
        return JavalinHttpResponse(response)
    }

    fun getStatus(path: String) = HttpStatus.forStatus(get(path).status)
    fun getBody(path: String) = get(path).body
    fun getBody(path: String, headers: Map<String, String>) = get(path, headers).body

    fun post(path: String) = PostRequestBuilder(origin + path, cookieManager, followRedirects)

    fun call(method: HttpMethod, pathname: String): JavalinHttpResponse {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(origin + pathname))
            .method(method.name(), HttpRequest.BodyPublishers.noBody())
            .build()
        
        val response = createClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
        return JavalinHttpResponse(response)
    }

    fun call(methodName: String, pathname: String): JavalinHttpResponse {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(origin + pathname))
            .method(methodName, HttpRequest.BodyPublishers.noBody())
            .build()
        
        val response = createClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString())  
        return JavalinHttpResponse(response)
    }

    fun htmlGet(path: String): JavalinHttpResponse = get(path, mapOf("Accept" to ContentType.HTML))
    fun jsonGet(path: String): JavalinHttpResponse = get(path, mapOf("Accept" to ContentType.JSON))

    fun sse(path: String): CompletableFuture<JavalinHttpResponse> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(origin + path))
            .header("Accept", "text/event-stream")
            .header("Connection", "keep-alive")
            .header("Cache-Control", "no-cache")
            .GET()
            .build()
        
        return createClient().sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
            .thenApply { JavalinHttpResponse(it) }
    }

    fun wsUpgradeRequest(path: String): JavalinHttpResponse = get(path, mapOf(Header.SEC_WEBSOCKET_KEY to "not-null"))
}

// Wrapper classes to maintain API compatibility
class JavalinHttpResponse(private val response: java.net.http.HttpResponse<String>) {
    val status: Int get() = response.statusCode()
    val body: String get() = response.body()
    val headers: HttpHeaders get() = HttpHeaders(response.headers())
}

class HttpHeaders(private val headers: java.net.http.HttpHeaders) {
    fun getFirst(name: String): String? = headers.firstValue(name).orElse(null)
    operator fun get(name: String): List<String>? = headers.allValues(name).takeIf { it.isNotEmpty() }
}

class PostRequestBuilder(
    private val url: String,
    private val cookieManager: CookieManager,
    private val followRedirects: Boolean
) {
    private val headers = mutableMapOf<String, String>()
    private var bodyContent: String? = null
    private val queryParams = mutableListOf<Pair<String, String>>()

    fun header(name: String, value: String): PostRequestBuilder {
        headers[name] = value
        return this
    }

    fun contentType(contentType: String): PostRequestBuilder {
        headers["Content-Type"] = contentType
        return this
    }

    fun queryString(name: String, value: String): PostRequestBuilder {
        queryParams.add(name to value)
        return this
    }

    fun body(content: String): PostRequestBuilder {
        bodyContent = content
        return this
    }

    fun body(content: ByteArray): PostRequestBuilder {
        bodyContent = String(content)
        return this
    }

    fun asString(): JavalinHttpResponse {
        val finalUrl = if (queryParams.isNotEmpty()) {
            val queryString = queryParams.joinToString("&") { (name, value) ->
                "${java.net.URLEncoder.encode(name, "UTF-8")}=${java.net.URLEncoder.encode(value, "UTF-8")}"
            }
            "$url?$queryString"
        } else {
            url
        }
        
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(finalUrl))
            .POST(HttpRequest.BodyPublishers.ofString(bodyContent ?: ""))
        
        headers.forEach { (name, value) -> requestBuilder.header(name, value) }
        
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .cookieHandler(cookieManager)
            .followRedirects(if (followRedirects) HttpClient.Redirect.NORMAL else HttpClient.Redirect.NEVER)
            .build()
        
        val response = client.send(requestBuilder.build(), java.net.http.HttpResponse.BodyHandlers.ofString())
        return JavalinHttpResponse(response)
    }
}

fun JavalinHttpResponse.httpCode(): HttpStatus = HttpStatus.forStatus(this.status)

// Compatibility with kong.unirest.HttpResponse 
fun kong.unirest.HttpResponse<*>.httpCode(): HttpStatus = HttpStatus.forStatus(this.status)
fun kong.unirest.HttpResponse<String>.header(name: String): String = this.headers.getFirst(name) ?: ""
val kong.unirest.HttpResponse<*>.allowHeader: String get() = this.headers[io.javalin.http.Header.ALLOW]!![0]
