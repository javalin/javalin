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
import kong.unirest.HttpResponse
import kong.unirest.Unirest
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse as JdkHttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CompletableFuture

class HttpUtil(port: Int) {

    init {
        if (!standardCookieHandlingSet) {
            // Use JDK HTTP client instead of Unirest for cookie handling
            standardCookieHandlingSet = true
        }
    }

    @JvmField
    val origin: String = "http://localhost:$port"
    
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun enableUnirestRedirects() {
        // JDK HTTP client follows redirects by default
    }
    
    fun disableUnirestRedirects() {
        // JDK HTTP client follows redirects by default  
    }

    // Use JDK HTTP client but wrap in Unirest-compatible response
    fun get(path: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(origin + path))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()
        val response = client.send(request, JdkHttpResponse.BodyHandlers.ofString())
        return HttpResponseAdapter(response)
    }
    
    fun get(path: String, headers: Map<String, String>): HttpResponse<String> {
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(origin + path))
            .timeout(Duration.ofSeconds(30))
            .GET()
        headers.forEach { (name, value) -> requestBuilder.header(name, value) }
        val response = client.send(requestBuilder.build(), JdkHttpResponse.BodyHandlers.ofString())
        return HttpResponseAdapter(response)
    }

    fun getStatus(path: String) = HttpStatus.forStatus(get(path).status)
    fun getBody(path: String) = get(path).body
    fun getBody(path: String, headers: Map<String, String>) = get(path, headers).body
    
    fun post(path: String) = RequestBuilderAdapter(client, origin + path, "POST")
    
    fun call(method: HttpMethod, pathname: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(origin + pathname))
            .timeout(Duration.ofSeconds(30))
            .method(method.name, HttpRequest.BodyPublishers.noBody())
            .build()
        val response = client.send(request, JdkHttpResponse.BodyHandlers.ofString())
        return HttpResponseAdapter(response)
    }
    
    fun htmlGet(path: String): HttpResponse<String> = get(path, mapOf("Accept" to ContentType.HTML))
    fun jsonGet(path: String): HttpResponse<String> = get(path, mapOf("Accept" to ContentType.JSON))
    
    fun sse(path: String): CompletableFuture<HttpResponse<String>> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(origin + path))
            .timeout(Duration.ofSeconds(30))
            .header("Accept", "text/event-stream")
            .header("Connection", "keep-alive")
            .header("Cache-Control", "no-cache")
            .GET()
            .build()
        return client.sendAsync(request, JdkHttpResponse.BodyHandlers.ofString())
            .thenApply { HttpResponseAdapter(it) }
    }
    
    fun wsUpgradeRequest(path: String): HttpResponse<String> = 
        get(path, mapOf(Header.SEC_WEBSOCKET_KEY to "not-null"))

    companion object {
        var standardCookieHandlingSet = false
    }
}

// Adapter that implements the kong.unirest.HttpResponse interface using JDK HTTP client
class HttpResponseAdapter(private val response: JdkHttpResponse<String>) : HttpResponse<String> {
    
    override fun getStatus(): Int = response.statusCode()
    override fun getStatusText(): String = ""
    override fun getHeaders(): kong.unirest.Headers = HeadersAdapter(response.headers())
    override fun getBody(): String = response.body()
    override fun getParsingError(): Exception? = null
    override fun isSuccess(): Boolean = response.statusCode() in 200..299
    
    // Implement missing methods from HttpResponse interface
    override fun getCookies(): kong.unirest.Cookies = TODO("Not implemented")
    override fun getContentType(): String = response.headers().firstValue("Content-Type").orElse("")
    override fun getEncoding(): String = "UTF-8"
    override fun ifSuccess(consumer: java.util.function.Consumer<HttpResponse<String>>): HttpResponse<String> {
        if (isSuccess()) consumer.accept(this)
        return this
    }
    override fun ifFailure(consumer: java.util.function.Consumer<HttpResponse<String>>): HttpResponse<String> {
        if (!isSuccess()) consumer.accept(this)
        return this
    }
    override fun ifFailure(statusCode: Int, consumer: java.util.function.Consumer<HttpResponse<String>>): HttpResponse<String> {
        if (getStatus() == statusCode) consumer.accept(this)
        return this
    }
    override fun ifServerError(consumer: java.util.function.Consumer<HttpResponse<String>>): HttpResponse<String> {
        if (getStatus() >= 500) consumer.accept(this)
        return this
    }
    override fun ifClientError(consumer: java.util.function.Consumer<HttpResponse<String>>): HttpResponse<String> {
        if (getStatus() in 400..499) consumer.accept(this)
        return this
    }
    override fun <V> map(function: java.util.function.Function<HttpResponse<String>, V>): V = function.apply(this)
    override fun <V> mapError(function: java.util.function.Function<HttpResponse<String>, V>): V = function.apply(this)
    override fun <V> mapBody(function: java.util.function.Function<String, V>): HttpResponse<V> = TODO("Not implemented")
    
    // Additional properties for compatibility
    val status: Int get() = getStatus()
    val body: String get() = getBody()
    val headers: kong.unirest.Headers get() = getHeaders()
}

// Adapter for headers
class HeadersAdapter(private val headers: java.net.http.HttpHeaders) : kong.unirest.Headers {
    override fun all(): MutableList<kong.unirest.Header> = TODO("Not implemented")
    override fun size(): Int = headers.map().size
    override fun getFirst(name: String): String? = headers.firstValue(name).orElse(null)
    override fun containsKey(name: String): Boolean = headers.map().containsKey(name)
    override fun get(name: String): MutableList<String> = headers.allValues(name).toMutableList()
    override fun add(name: String, value: String) = TODO("Not implemented")
    override fun add(header: kong.unirest.Header) = TODO("Not implemented")  
    override fun replace(name: String, value: String) = TODO("Not implemented")
    override fun remove(name: String) = TODO("Not implemented")
    override fun clear() = TODO("Not implemented")
    override fun putAll(headers: kong.unirest.Headers) = TODO("Not implemented")
    override fun isEmpty(): Boolean = headers.map().isEmpty()
}

// Request builder adapter
class RequestBuilderAdapter(
    private val client: HttpClient, 
    private var url: String,
    private val method: String
) {
    private val headers = mutableMapOf<String, String>()
    private var requestBody: String? = null
    
    fun header(name: String, value: String): RequestBuilderAdapter {
        headers[name] = value
        return this
    }
    
    fun headers(headers: Map<String, String>): RequestBuilderAdapter {
        this.headers.putAll(headers)
        return this
    }
    
    fun body(body: String): RequestBuilderAdapter {
        this.requestBody = body
        return this
    }
    
    fun body(body: ByteArray): RequestBuilderAdapter {
        this.requestBody = String(body, StandardCharsets.UTF_8)
        return this
    }
    
    fun contentType(contentType: String): RequestBuilderAdapter {
        headers["Content-Type"] = contentType
        return this
    }
    
    fun basicAuth(username: String, password: String): RequestBuilderAdapter {
        val credentials = "$username:$password"
        val encoded = java.util.Base64.getEncoder().encodeToString(credentials.toByteArray(StandardCharsets.UTF_8))
        headers["Authorization"] = "Basic $encoded"
        return this
    }
    
    fun queryString(name: String, value: String): RequestBuilderAdapter {
        val separator = if (url.contains("?")) "&" else "?"
        val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8)
        val encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8)
        url += "$separator$encodedName=$encodedValue"
        return this
    }
    
    fun field(name: String, value: String): RequestBuilderAdapter {
        if (requestBody == null) {
            requestBody = ""
            headers["Content-Type"] = "application/x-www-form-urlencoded"
        } else if (requestBody!!.isNotEmpty()) {
            requestBody += "&"
        }
        requestBody += "${URLEncoder.encode(name, StandardCharsets.UTF_8)}=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
        return this
    }
    
    fun asString(): HttpResponse<String> {
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
        
        headers.forEach { (name, value) -> requestBuilder.header(name, value) }
        
        val bodyPublisher = when {
            requestBody != null -> HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8)
            method in listOf("POST", "PUT", "PATCH") -> HttpRequest.BodyPublishers.ofString("")
            else -> HttpRequest.BodyPublishers.noBody()
        }
        
        requestBuilder.method(method, bodyPublisher)
        val response = client.send(requestBuilder.build(), JdkHttpResponse.BodyHandlers.ofString())
        return HttpResponseAdapter(response)
    }
    
    fun asStringAsync(): CompletableFuture<HttpResponse<String>> {
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
        
        headers.forEach { (name, value) -> requestBuilder.header(name, value) }
        
        val bodyPublisher = when {
            requestBody != null -> HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8)
            method in listOf("POST", "PUT", "PATCH") -> HttpRequest.BodyPublishers.ofString("")
            else -> HttpRequest.BodyPublishers.noBody()
        }
        
        requestBuilder.method(method, bodyPublisher)
        return client.sendAsync(requestBuilder.build(), JdkHttpResponse.BodyHandlers.ofString())
            .thenApply { HttpResponseAdapter(it) }
    }
}

fun HttpResponse<*>.httpCode(): HttpStatus =
    HttpStatus.forStatus(this.status)
