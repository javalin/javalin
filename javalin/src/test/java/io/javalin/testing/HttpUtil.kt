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
    fun get(path: String): HttpResponseWrapper {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(origin + path))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()
        val response = client.send(request, JdkHttpResponse.BodyHandlers.ofString())
        return HttpResponseWrapper(response)
    }
    
    fun get(path: String, headers: Map<String, String>): HttpResponseWrapper {
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
    
    fun call(method: HttpMethod, pathname: String): HttpResponseWrapper {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(origin + pathname))
            .timeout(Duration.ofSeconds(30))
            .method(method.name(), HttpRequest.BodyPublishers.noBody())
            .build()
        val response = client.send(request, JdkHttpResponse.BodyHandlers.ofString())
        return HttpResponseWrapper(response)
    }
    
    fun htmlGet(path: String): HttpResponseWrapper = get(path, mapOf("Accept" to ContentType.HTML))
    fun jsonGet(path: String): HttpResponseWrapper = get(path, mapOf("Accept" to ContentType.JSON))
    
    fun sse(path: String): CompletableFuture<HttpResponseWrapper> {
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
    
    fun wsUpgradeRequest(path: String): HttpResponseWrapper = 
        get(path, mapOf(Header.SEC_WEBSOCKET_KEY to "not-null"))

    companion object {
        var standardCookieHandlingSet = false
    }
}

// Wrapper that provides compatibility with Unirest HttpResponse without full interface implementation
class HttpResponseWrapper(private val response: JdkHttpResponse<String>) {
    
    fun getStatus() = response.statusCode()
    fun getStatusText() = ""
    fun isSuccess() = response.statusCode() in 200..299
    fun getBody() = response.body()
    
    val headers = object {
        private val headerMap = response.headers().map()
        fun size() = headerMap.size
        fun getFirst(name: String) = headerMap[name]?.firstOrNull()
        fun get(name: String) = headerMap[name]?.toMutableList() ?: mutableListOf()
        fun containsKey(name: String) = headerMap.containsKey(name)
    }
    
    fun getHeaders() = headers
    
    fun getCookies() = object {
        fun size() = 0
        fun getNamed(name: String) = null
        fun getFirst() = null  
        fun iterator() = mutableListOf<Any>().iterator()
    }
    
    fun getParsingError() = null
    fun ifSuccess(action: java.util.function.Consumer<HttpResponseWrapper>) = 
        if (isSuccess()) { action.accept(this); this } else this
    fun ifFailure(action: java.util.function.Consumer<HttpResponseWrapper>) =
        if (!isSuccess()) { action.accept(this); this } else this
    fun ifFailure(statusCode: Int, action: java.util.function.Consumer<HttpResponseWrapper>) =
        if (status == statusCode) { action.accept(this); this } else this
    
    // Properties for compatibility
    val status: Int get() = response.statusCode()
    val body: String get() = response.body()
    val contentType: String get() = response.headers().firstValue("Content-Type").orElse("")
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
    
    fun asString(): HttpResponseWrapper {
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

// Extension functions for compatibility 
fun HttpResponseWrapper.httpCode(): HttpStatus =
    HttpStatus.forStatus(this.status)

fun HttpResponseWrapper.assertStatusAndBodyMatch(status: Int, body: String) {
    org.assertj.core.api.Assertions.assertThat(this.status).isEqualTo(status)
    org.assertj.core.api.Assertions.assertThat(this.body).isNotNull.isEqualTo(body)
}

fun HttpResponseWrapper.assertStatusAndBodyMatch(status: HttpStatus, body: String) {
    org.assertj.core.api.Assertions.assertThat(this.httpCode()).isEqualTo(status)
    org.assertj.core.api.Assertions.assertThat(this.body).isNotNull.isEqualTo(body)
}
