package io.javalin.testtools

import io.javalin.plugin.json.JavalinJson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.function.Consumer

class HttpClient(port: Int) {

    var okHttp = OkHttpClient()
    var origin: String = "http://localhost:$port"

    fun request(request: Request) = okHttp.newCall(request).execute()
    fun request(path: String, builder: Request.Builder) = request(builder.url(origin + path).build())
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
        request(path, combine(req, { it.post(json.toRequestBody()) }))

    @JvmOverloads
    fun put(path: String, json: Any? = null, req: Consumer<Request.Builder>? = null) =
            request(path, combine(req, { it.put(json.toRequestBody()) }))

    @JvmOverloads
    fun patch(path: String, json: Any? = null, req: Consumer<Request.Builder>? = null) =
            request(path, combine(req, { it.patch(json.toRequestBody()) }))

    @JvmOverloads
    fun delete(path: String, json: Any? = null, req: Consumer<Request.Builder>? = null) =
            request(path, combine(req, { it.delete(json.toRequestBody()) }))

    companion object {
        private val JSON_TYPE = "application/json".toMediaType()
        private fun Any?.toRequestBody(): RequestBody {
            return if (this == null) {
                ByteArray(0).toRequestBody(null, 0, 0)
            } else {
                JavalinJson.toJson(this).toRequestBody(JSON_TYPE)
            }
        }
    }

    private fun combine(userBuilder: Consumer<Request.Builder>?, utilBuilder: Consumer<Request.Builder>): Request.Builder {
        val finalBuilder = Request.Builder()
        userBuilder?.accept(finalBuilder)
        utilBuilder.accept(finalBuilder)
        return finalBuilder
    }
}
