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

class HttpUtil(port: Int) {

    init {
        if (!standardCookieHandlingSet) {
            Unirest.config().cookieSpec("standard") // handles cookies according to RFC 6265
            standardCookieHandlingSet = true
        }
    }

    @JvmField
    val origin: String = "http://localhost:$port"

    fun enableUnirestRedirects() = Unirest.config().reset().followRedirects(true)
    fun disableUnirestRedirects() = Unirest.config().reset().followRedirects(false)

    // Unirest
    fun get(path: String) = Unirest.get(origin + path).asString()
    fun get(path: String, headers: Map<String, String>) = Unirest.get(origin + path).headers(headers).asString()

    fun getStatus(path: String) = HttpStatus.forStatus(get(path).status)
    fun getBody(path: String) = Unirest.get(origin + path).asString().body
    fun getBody(path: String, headers: Map<String, String>) = Unirest.get(origin + path).headers(headers).asString().body
    fun post(path: String) = Unirest.post(origin + path)
    fun query(path: String) = Unirest.request("QUERY", origin + path)
    fun call(method: HttpMethod, pathname: String) = Unirest.request(method.name(), origin + pathname).asString()
    fun htmlGet(path: String) = Unirest.get(origin + path).header("Accept", ContentType.HTML).asString()
    fun jsonGet(path: String) = Unirest.get(origin + path).header("Accept", ContentType.JSON).asString()
    fun sse(path: String) = Unirest.get(origin + path).header("Accept", "text/event-stream").header("Connection", "keep-alive").header("Cache-Control", "no-cache").asStringAsync()
    fun wsUpgradeRequest(path: String) =Unirest.get(origin + path).header(Header.SEC_WEBSOCKET_KEY, "not-null").asString()

    companion object {
        var standardCookieHandlingSet = false
    }
}

fun HttpResponse<*>.httpCode(): HttpStatus =
    HttpStatus.forStatus(this.status)
