package io.javalin.testtools

import io.javalin.Javalin
import io.javalin.http.ContentType
import io.javalin.json.toJsonString
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.function.Consumer
import java.net.http.HttpClient as JdkHttpClient
import java.net.http.HttpHeaders as JdkHttpHeaders

// Wrapper classes to maintain compatibility with OkHTTP API

class FormBody private constructor(private val formData: String) {
    fun toBodyPublisher(): HttpRequest.BodyPublisher {
        return HttpRequest.BodyPublishers.ofString(formData, StandardCharsets.UTF_8)
    }

    class Builder {
        private val params = mutableListOf<Pair<String, String>>()

        fun add(name: String, value: String): Builder {
            params.add(name to value)
            return this
        }

        fun build(): FormBody {
            val formData = params.joinToString("&") { (name, value) ->
                "${URLEncoder.encode(name, StandardCharsets.UTF_8)}=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
            }
            return FormBody(formData)
        }
    }
}

class Response(private val response: HttpResponse<String>) {
    fun code(): Int = response.statusCode()
    val code: Int get() = response.statusCode()

    fun body(): ResponseBody = ResponseBody(response.body())
    val body: ResponseBody get() = ResponseBody(response.body())

    fun headers(): HttpHeaders = HttpHeaders(response.headers())
}

class HttpHeaders(private val headers: JdkHttpHeaders) {
    fun get(name: String): List<String>? = headers.allValues(name).takeIf { it.isNotEmpty() }
}

class ResponseBody(private val bodyContent: String) {
    fun string(): String = bodyContent
}

class Request private constructor(
    val url: String,
    val method: String,
    val headers: Map<String, String>,
    val bodyPublisher: HttpRequest.BodyPublisher?
) {
    class Builder {
        private var url: String = ""
        private var method: String = "GET"
        private val headers = mutableMapOf<String, String>()
        private var bodyPublisher: HttpRequest.BodyPublisher? = null

        fun url(url: String): Builder {
            this.url = url
            return this
        }

        fun header(name: String, value: String): Builder {
            headers[name] = value
            return this
        }

        fun get(): Builder {
            method = "GET"
            bodyPublisher = null
            return this
        }

        fun post(body: HttpRequest.BodyPublisher): Builder {
            method = "POST"
            bodyPublisher = body
            return this
        }

        fun post(formBody: FormBody): Builder {
            method = "POST"
            bodyPublisher = formBody.toBodyPublisher()
            header("Content-Type", "application/x-www-form-urlencoded")
            return this
        }

        fun put(body: HttpRequest.BodyPublisher): Builder {
            method = "PUT"
            bodyPublisher = body
            return this
        }

        fun patch(body: HttpRequest.BodyPublisher): Builder {
            method = "PATCH"
            bodyPublisher = body
            return this
        }

        fun delete(body: HttpRequest.BodyPublisher? = null): Builder {
            method = "DELETE"
            bodyPublisher = body
            return this
        }

        fun build(): Request {
            return Request(url, method, headers.toMap(), bodyPublisher)
        }
    }
}

class HttpClient(val app: Javalin, private val client: JdkHttpClient) {

    var origin: String = "http://127.0.0.1:${app.port()}"
    private val defaultHeaders = mutableMapOf<String, String>()

    constructor(app: Javalin, client: JdkHttpClient, defaultHeaders: Map<String, String>) : this(app, client) {
        this.defaultHeaders.putAll(defaultHeaders)
    }

    fun request(request: Request): Response {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create(request.url))
            .method(request.method, request.bodyPublisher ?: HttpRequest.BodyPublishers.noBody())

        // Add default headers first
        defaultHeaders.forEach { (name, value) ->
            builder.header(name, value)
        }

        // Then add request-specific headers (these can override defaults)
        request.headers.forEach { (name, value) ->
            builder.header(name, value)
        }

        val httpRequest = builder.build()
        val response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString())
        return Response(response)
    }

    fun request(path: String, builder: Request.Builder): Response = request(builder.url(origin + path).build())

    fun request(path: String, userBuilder: Consumer<Request.Builder>): Response {
        val finalBuilder = Request.Builder()
        userBuilder.accept(finalBuilder)
        return request(finalBuilder.url(origin + path).build())
    }

    @JvmOverloads
    fun get(path: String, req: Consumer<Request.Builder>? = null) =
            request(path, combine(req, { it.get() }))

    @JvmOverloads
    fun post(path: String, json: Any? = null, req: Consumer<Request.Builder>? = null) =
        request(path, combine(req, { builder ->
            builder.post(json.toRequestBodyPublisher())
            if (json != null) {
                builder.header("Content-Type", ContentType.JSON)
            }
        }))

    @JvmOverloads
    fun put(path: String, json: Any? = null, req: Consumer<Request.Builder>? = null) =
            request(path, combine(req, { builder ->
                builder.put(json.toRequestBodyPublisher())
                if (json != null) {
                    builder.header("Content-Type", ContentType.JSON)
                }
            }))

    @JvmOverloads
    fun patch(path: String, json: Any? = null, req: Consumer<Request.Builder>? = null) =
            request(path, combine(req, { builder ->
                builder.patch(json.toRequestBodyPublisher())
                if (json != null) {
                    builder.header("Content-Type", ContentType.JSON)
                }
            }))

    @JvmOverloads
    fun delete(path: String, json: Any? = null, req: Consumer<Request.Builder>? = null) =
            request(path, combine(req, { builder ->
                builder.delete(json.toRequestBodyPublisher())
                if (json != null) {
                    builder.header("Content-Type", ContentType.JSON)
                }
            }))

    private fun Any?.toRequestBodyPublisher(): HttpRequest.BodyPublisher {
        return if (this == null) {
            HttpRequest.BodyPublishers.noBody()
        } else {
            val jsonString = app.unsafe.jsonMapper.value.toJsonString(this)
            HttpRequest.BodyPublishers.ofString(jsonString, StandardCharsets.UTF_8)
        }
    }

    private fun combine(userBuilder: Consumer<Request.Builder>?, utilBuilder: Consumer<Request.Builder>): Consumer<Request.Builder> {
        return Consumer { finalBuilder ->
            userBuilder?.accept(finalBuilder)
            utilBuilder.accept(finalBuilder)
        }
    }
}
