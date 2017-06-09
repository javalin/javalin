/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.util

import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.config.RegistryBuilder
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.BasicHttpClientConnectionManager
import org.apache.http.util.EntityUtils
import java.io.IOException

class SimpleHttpClient {

    private val httpClient: HttpClient = HttpClientBuilder.create().setConnectionManager(
            BasicHttpClientConnectionManager(
                    RegistryBuilder.create<ConnectionSocketFactory>()
                            .register("http", PlainConnectionSocketFactory.INSTANCE)
                            .build()
            )
    ).build()

    @Throws(IOException::class)
    fun http_GET(path: String): TestResponse {
        val httpResponse = httpClient.execute(HttpGet(path))
        return TestResponse(EntityUtils.toString(httpResponse.entity), httpResponse.statusLine.statusCode)
    }

}

data class TestResponse(var body: String, var status: Int)
