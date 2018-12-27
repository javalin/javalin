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
import io.javalin.core.util.Header
import org.apache.http.impl.client.HttpClients

class HttpUtil(javalin: Javalin) {

    @JvmField
    val origin: String = "http://localhost:" + javalin.port()

    fun enableUnirestRedirects() = Unirest.setHttpClient(HttpClients.custom().build())
    fun disableUnirestRedirects() = Unirest.setHttpClient(HttpClients.custom().disableRedirectHandling().build())

    // Unirest

    fun get(path: String) = Unirest.get(origin + path).asString()
    fun getBody(path: String) = Unirest.get(origin + path).asString().body
    fun post(path: String) = Unirest.post(origin + path)
    fun call(method: HttpMethod, pathname: String) = HttpRequestWithBody(method, origin + pathname).asString()
    fun htmlGet(path: String) = Unirest.get(origin + path).header(Header.ACCEPT, "text/html").asString()
    fun jsonGet(path: String) = Unirest.get(origin + path).header(Header.ACCEPT, "application/json").asString()

}
