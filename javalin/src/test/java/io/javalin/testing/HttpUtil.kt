/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.testing

import com.mashape.unirest.http.HttpMethod
import com.mashape.unirest.http.Unirest
import com.mashape.unirest.request.HttpRequestWithBody
import org.apache.http.impl.client.HttpClients

class HttpUtil(port: Int) {

    @JvmField
    val origin: String = "http://localhost:$port"

    fun enableUnirestRedirects() = Unirest.setHttpClient(HttpClients.custom().build())
    fun disableUnirestRedirects() = Unirest.setHttpClient(HttpClients.custom().disableRedirectHandling().build())

    // Unirest
    fun get(path: String) = Unirest.get(origin + path).asString()

    fun getBody(path: String) = Unirest.get(origin + path).asString().body
    fun getBody(path: String, headers: Map<String, String>) = Unirest.get(origin + path).headers(headers).asString().body
    fun post(path: String) = Unirest.post(origin + path)
    fun call(method: HttpMethod, pathname: String) = HttpRequestWithBody(method, origin + pathname).asString()
    fun htmlGet(path: String) = Unirest.get(origin + path).header("Accept", "text/html").asString()
    fun jsonGet(path: String) = Unirest.get(origin + path).header("Accept", "application/json").asString()
    fun sse(path: String) = Unirest.get(origin + path).header("Accept", "text/event-stream").header("Connection", "keep-alive").header("Cache-Control", "no-cache").asStringAsync()

}
