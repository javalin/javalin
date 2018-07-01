/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.util

import com.mashape.unirest.http.HttpMethod
import com.mashape.unirest.http.Unirest
import com.mashape.unirest.request.HttpRequestWithBody
import io.javalin.Javalin
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.http.impl.client.HttpClients
import java.util.concurrent.TimeUnit

class HttpUtil(javalin: Javalin) {
    private val okHttp = OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    @JvmField
    val origin: String = "http://localhost:" + javalin.port()

    fun enableUnirestRedirects() = Unirest.setHttpClient(HttpClients.custom().build())
    fun disableUnirestRedirects() = Unirest.setHttpClient(HttpClients.custom().disableRedirectHandling().build())

    // OkHTTP
    fun get(path: String) = okHttp.newCall(Request.Builder().url(origin + path).get().build()).execute()
    fun getBody(path: String) = get(path).body()!!.string()

    // Unirest
    fun call(method: HttpMethod, pathname: String) = HttpRequestWithBody(method, origin + pathname).asString()
    fun post(path: String) = Unirest.post(origin + path)
    fun getBody_withCookies(path: String) = Unirest.get(origin + path).asString().body
    fun get_withCookies(path: String) = Unirest.get(origin + path).asString()

}
