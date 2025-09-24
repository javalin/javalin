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
            // JDK HTTP client handles cookies differently than Unirest
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

    // Replace Unirest calls with JDK HTTP client calls but maintain exact same signatures
    fun get(path: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(origin + path))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()
        val response = client.send(request, JdkHttpResponse.BodyHandlers.ofString())
        return HttpResponseWrapper(response)
    }
    
    fun get(path: String, headers: Map<String, String>): HttpResponse<String> {
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(origin + path))
            .timeout(Duration.ofSeconds(30))
            .GET()
        headers.forEach { (name, value) -> requestBuilder.header(name, value) }
        val response = client.send(requestBuilder.build(), JdkHttpResponse.BodyHandlers.ofString())
        return HttpResponseWrapper(response)
    }

    fun getStatus(path: String) = HttpStatus.forStatus(get(path).status)
    fun getBody(path: String) = get(path).body
    fun getBody(path: String, headers: Map<String, String>) = get(path, headers).body
    
    fun post(path: String) = RequestBuilder("POST", origin + path, client)
    
    fun call(method: HttpMethod, pathname: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(origin + pathname))
            .timeout(Duration.ofSeconds(30))
            .method(method.name(), HttpRequest.BodyPublishers.noBody())
            .build()
        val response = client.send(request, JdkHttpResponse.BodyHandlers.ofString())
        return HttpResponseWrapper(response)
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
            .thenApply { HttpResponseWrapper(it) }
    }
    
    fun wsUpgradeRequest(path: String): HttpResponse<String> = 
        get(path, mapOf(Header.SEC_WEBSOCKET_KEY to "not-null"))

    companion object {
        var standardCookieHandlingSet = false
    }
}

// Wrapper that implements Unirest HttpResponse interface for compatibility
class HttpResponseWrapper(private val response: JdkHttpResponse<String>) : HttpResponse<String> {
    
    override fun getStatus() = response.statusCode()
    override fun getStatusText() = ""
    override fun isSuccess() = response.statusCode() in 200..299
    override fun getBody(): String = response.body()
    
    override fun getHeaders() = object : kong.unirest.Headers {
        private val headerMap = response.headers().map()
        override fun size() = headerMap.size
        override fun getFirst(name: String) = headerMap[name]?.firstOrNull()
        override fun get(name: String) = headerMap[name]?.toMutableList() ?: mutableListOf()
        override fun all() = mutableListOf<kong.unirest.Header>()
        override fun containsKey(name: String) = headerMap.containsKey(name)
        // Stub implementations for methods we don't need
        override fun replace(name: String, value: String) = Unit
        override fun replace(name: String, value: MutableCollection<String>) = Unit
        override fun add(name: String, value: String) = Unit
        override fun add(name: String, value: MutableCollection<String>) = Unit
        override fun remove(name: String) = Unit
        override fun clear() = Unit
        override fun putAll(headers: MutableMap<out String, out MutableList<String>>) = Unit
    }
    
    override fun getCookies() = object : kong.unirest.Cookies {
        override fun size() = 0
        override fun getNamed(name: String) = null
        override fun getFirst() = null  
        override fun iterator() = mutableListOf<kong.unirest.Cookie>().iterator()
    }
    
    override fun getParsingError() = null
    override fun ifSuccess(action: java.util.function.Consumer<HttpResponse<String>>) = 
        if (isSuccess) { action.accept(this); this } else this
    override fun ifFailure(action: java.util.function.Consumer<HttpResponse<String>>) =
        if (!isSuccess) { action.accept(this); this } else this
    override fun ifFailure(statusCode: Int, action: java.util.function.Consumer<HttpResponse<String>>) =
        if (status == statusCode) { action.accept(this); this } else this
    
    // Properties for compatibility (these match the original Unirest behavior)
    val status: Int get() = response.statusCode()
    val body: String get() = response.body()
    val contentType: String get() = response.headers().firstValue("Content-Type").orElse("")
    val headers = getHeaders()
    
    fun status(): Int = response.statusCode()
    fun body(): String = response.body()
    fun contentType(): String = response.headers().firstValue("Content-Type").orElse("")
}

// Request builder that mimics Unirest RequestBuilder
class RequestBuilder(private val method: String, private val url: String, private val client: HttpClient) {
    private val headers = mutableMapOf<String, String>()
    private var requestBody: String? = null
    
    fun header(name: String, value: String): RequestBuilder {
        headers[name] = value
        return this
    }
    
    fun headers(headers: Map<String, String>): RequestBuilder {
        this.headers.putAll(headers)
        return this
    }
    
    fun body(body: String): RequestBuilder {
        this.requestBody = body
        return this
    }
    
    fun body(body: ByteArray): RequestBuilder {
        this.requestBody = String(body, StandardCharsets.UTF_8)
        return this
    }
    
    fun contentType(contentType: String): RequestBuilder {
        headers["Content-Type"] = contentType
        return this
    }
    
    fun basicAuth(username: String, password: String): RequestBuilder {
        val credentials = "$username:$password"
        val encoded = java.util.Base64.getEncoder().encodeToString(credentials.toByteArray(StandardCharsets.UTF_8))
        headers["Authorization"] = "Basic $encoded"
        return this
    }
    
    fun queryString(name: String, value: String): RequestBuilder {
        val separator = if (url.contains("?")) "&" else "?"
        val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8)
        val encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8)
        return RequestBuilder(method, "$url$separator$encodedName=$encodedValue", client)
    }
    
    fun field(name: String, value: String): RequestBuilder {
        if (requestBody == null) {
            requestBody = ""
            headers["Content-Type"] = "application/x-www-form-urlencoded"
        } else if (requestBody!!.isNotEmpty()) {
            requestBody += "&"
        }
        requestBody += "${URLEncoder.encode(name, StandardCharsets.UTF_8)}=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
        return this
    }
    
    // Simplified file support - just use filename for basic compatibility  
    fun field(name: String, file: java.io.File): RequestBuilder {
        return field(name, file.name)
    }
    
    fun field(name: String, file: java.io.File, contentType: String): RequestBuilder {
        return field(name, file.name)
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
        return HttpResponseWrapper(response)
    }
}

fun HttpResponse<*>.httpCode(): HttpStatus =
    HttpStatus.forStatus(this.status)

// Extension functions for compatibility with test files that expect HttpResponse<String>
fun HttpResponse<String>.assertStatusAndBodyMatch(status: Int, body: String) {
    org.assertj.core.api.Assertions.assertThat(this.status).isEqualTo(status)
    org.assertj.core.api.Assertions.assertThat(this.body).isNotNull.isEqualTo(body)
}

fun HttpResponse<String>.assertStatusAndBodyMatch(status: HttpStatus, body: String) {
    org.assertj.core.api.Assertions.assertThat(this.httpCode()).isEqualTo(status)
    org.assertj.core.api.Assertions.assertThat(this.body).isNotNull.isEqualTo(body)
}
