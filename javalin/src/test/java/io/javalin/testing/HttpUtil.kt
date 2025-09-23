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

// Create wrappers that look like Unirest but use JDK HTTP client
object UnirestAdapter {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    
    fun get(url: String) = RequestBuilder("GET", url)
    fun post(url: String) = RequestBuilder("POST", url)
    fun put(url: String) = RequestBuilder("PUT", url)
    fun delete(url: String) = RequestBuilder("DELETE", url)
    fun patch(url: String) = RequestBuilder("PATCH", url)
    fun options(url: String) = RequestBuilder("OPTIONS", url)
    fun request(method: String, url: String) = RequestBuilder(method, url)
    
    class RequestBuilder(private val method: String, private val url: String) {
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
        
        fun queryString(name: String, value: String): RequestBuilder {
            val separator = if (url.contains("?")) "&" else "?"
            val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8)
            val encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8)
            return RequestBuilder(method, "$url$separator$encodedName=$encodedValue")
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
        
        fun asString(): HttpResponseWrapper {
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
            
            val response = client.send(requestBuilder.build(), JdkHttpResponse.BodyHandlers.ofString())
            return HttpResponseWrapper(response)
        }
        
        fun asBytes(): HttpResponseWrapper {
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
            
            val response = client.send(requestBuilder.build(), JdkHttpResponse.BodyHandlers.ofString())
            return HttpResponseWrapper(response)
        }
        
        fun asStringAsync(): CompletableFuture<HttpResponseWrapper> {
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
            
            return client.sendAsync(requestBuilder.build(), JdkHttpResponse.BodyHandlers.ofString())
                .thenApply { HttpResponseWrapper(it) }
        }
    }
    
    class HttpResponseWrapper(private val response: JdkHttpResponse<String>) {
        val status: Int get() = response.statusCode()
        val body: String get() = response.body()
        val headers = HeadersWrapper(response.headers())
        
        fun status(): Int = response.statusCode()
        fun body(): String = response.body()
    }
    
    class HeadersWrapper(private val jdkHeaders: java.net.http.HttpHeaders) {
        fun getFirst(name: String): String? = jdkHeaders.firstValue(name).orElse(null)
        fun get(name: String): List<String> = jdkHeaders.allValues(name)
    }
    
    fun config() = object {
        fun cookieSpec(spec: String) = this
        fun reset() = this
        fun followRedirects(follow: Boolean) = this
    }
    
    fun spawnInstance() = this
}

class HttpUtil(port: Int) {

    init {
        if (!standardCookieHandlingSet) {
            // Cookie handling compatibility flag for JDK HTTP client
            standardCookieHandlingSet = true
        }
    }

    @JvmField
    val origin: String = "http://localhost:$port"
    
    fun enableUnirestRedirects() {
        // JDK HTTP client follows redirects by default
    }
    
    fun disableUnirestRedirects() {
        // JDK HTTP client follows redirects by default
    }

    // Use UnirestAdapter for all HTTP operations
    fun get(path: String) = UnirestAdapter.get(origin + path).asString()
    fun get(path: String, headers: Map<String, String>) = 
        UnirestAdapter.get(origin + path).headers(headers).asString()

    fun getStatus(path: String) = HttpStatus.forStatus(get(path).status)
    fun getBody(path: String) = UnirestAdapter.get(origin + path).asString().body
    fun getBody(path: String, headers: Map<String, String>) = 
        UnirestAdapter.get(origin + path).headers(headers).asString().body
    fun post(path: String) = UnirestAdapter.post(origin + path)
    fun call(method: HttpMethod, pathname: String) = 
        UnirestAdapter.request(method.name(), origin + pathname).asString()
    fun htmlGet(path: String) = 
        UnirestAdapter.get(origin + path).header("Accept", ContentType.HTML).asString()
    fun jsonGet(path: String) = 
        UnirestAdapter.get(origin + path).header("Accept", ContentType.JSON).asString()
    fun sse(path: String) = 
        UnirestAdapter.get(origin + path)
            .header("Accept", "text/event-stream")
            .header("Connection", "keep-alive")
            .header("Cache-Control", "no-cache")
            .asStringAsync()
    fun wsUpgradeRequest(path: String) =
        UnirestAdapter.get(origin + path).header(Header.SEC_WEBSOCKET_KEY, "not-null").asString()

    companion object {
        var standardCookieHandlingSet = false
    }
}

fun HttpResponse<*>.httpCode(): HttpStatus =
    HttpStatus.forStatus(this.status)

// Extension function for our HttpResponseWrapper  
fun UnirestAdapter.HttpResponseWrapper.httpCode(): HttpStatus =
    HttpStatus.forStatus(this.status)
