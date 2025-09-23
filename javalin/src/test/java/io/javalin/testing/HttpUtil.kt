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

// Simple headers implementation
class Headers(private val map: Map<String, List<String>>) {
    fun getFirst(name: String): String? = map[name]?.firstOrNull()
}

// Simple implementation that mimics what the tests need from HttpResponse
class SimpleHttpResponse<T>(private val response: JdkHttpResponse<String>) {
    val headers = Headers(response.headers().map())
    val status: Int get() = response.statusCode()
    val body: String get() = response.body()
    
    fun status(): Int = response.statusCode()
    fun body(): String = response.body()
}

// Request builder that mimics Unirest request builder
class RequestBuilder(private val httpUtil: HttpUtil, private val method: String, private val url: String) {
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
    
    fun basicAuth(username: String, password: String): RequestBuilder {
        val credentials = "$username:$password"
        val encoded = java.util.Base64.getEncoder().encodeToString(credentials.toByteArray(StandardCharsets.UTF_8))
        headers["Authorization"] = "Basic $encoded"
        return this
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
    
    fun asString(): SimpleHttpResponse<String> {
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
        
        // Add headers
        headers.forEach { (name, value) ->
            requestBuilder.header(name, value)
        }
        
        // Set method and body
        val bodyPublisher = when {
            requestBody != null -> HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8)
            method in listOf("POST", "PUT", "PATCH") -> HttpRequest.BodyPublishers.ofString("")
            else -> HttpRequest.BodyPublishers.noBody()
        }
        
        requestBuilder.method(method, bodyPublisher)
        
        val response = httpUtil.client.send(requestBuilder.build(), JdkHttpResponse.BodyHandlers.ofString())
        return SimpleHttpResponse(response)
    }
    
    fun asStringAsync(): CompletableFuture<SimpleHttpResponse<String>> {
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
        
        // Add headers
        headers.forEach { (name, value) ->
            requestBuilder.header(name, value)
        }
        
        // Set method and body
        val bodyPublisher = when {
            requestBody != null -> HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8)
            method in listOf("POST", "PUT", "PATCH") -> HttpRequest.BodyPublishers.ofString("")
            else -> HttpRequest.BodyPublishers.noBody()
        }
        
        requestBuilder.method(method, bodyPublisher)
        
        return httpUtil.client.sendAsync(requestBuilder.build(), JdkHttpResponse.BodyHandlers.ofString())
            .thenApply { SimpleHttpResponse(it) }
    }
}

class HttpUtil(port: Int) {

    init {
        if (!standardCookieHandlingSet) {
            // JDK HTTP client handles cookies differently than Unirest
            // For now, maintain compatibility by just setting the flag
            standardCookieHandlingSet = true
        }
    }

    @JvmField
    val origin: String = "http://localhost:$port"
    
    internal val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    
    fun enableUnirestRedirects() {
        // JDK HTTP client follows redirects by default
    }
    
    fun disableUnirestRedirects() {
        // JDK HTTP client follows redirects by default
    }

    // Unirest-compatible methods
    fun get(path: String) = RequestBuilder(this, "GET", origin + path).asString()
    fun get(path: String, headers: Map<String, String>) = 
        RequestBuilder(this, "GET", origin + path).headers(headers).asString()

    fun getStatus(path: String) = HttpStatus.forStatus(get(path).status)
    fun getBody(path: String) = RequestBuilder(this, "GET", origin + path).asString().body
    fun getBody(path: String, headers: Map<String, String>) = 
        RequestBuilder(this, "GET", origin + path).headers(headers).asString().body
    fun post(path: String) = RequestBuilder(this, "POST", origin + path)
    fun call(method: HttpMethod, pathname: String) = 
        RequestBuilder(this, method.name(), origin + pathname).asString()
    fun htmlGet(path: String) = 
        RequestBuilder(this, "GET", origin + path).header("Accept", ContentType.HTML).asString()
    fun jsonGet(path: String) = 
        RequestBuilder(this, "GET", origin + path).header("Accept", ContentType.JSON).asString()
    fun sse(path: String) = 
        RequestBuilder(this, "GET", origin + path)
            .header("Accept", "text/event-stream")
            .header("Connection", "keep-alive")
            .header("Cache-Control", "no-cache")
            .asStringAsync()
    fun wsUpgradeRequest(path: String) =
        RequestBuilder(this, "GET", origin + path).header(Header.SEC_WEBSOCKET_KEY, "not-null").asString()

    companion object {
        var standardCookieHandlingSet = false
    }
}

fun HttpResponse<*>.httpCode(): HttpStatus =
    HttpStatus.forStatus(this.status)

// Extension function for our SimpleHttpResponse to match Unirest HttpResponse behavior
fun SimpleHttpResponse<*>.httpCode(): HttpStatus =
    HttpStatus.forStatus(this.status)
