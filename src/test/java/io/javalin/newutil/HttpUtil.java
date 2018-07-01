/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.newutil;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.HttpRequestWithBody;
import io.javalin.Javalin;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpUtil {

    private final OkHttpClient okHttp = new OkHttpClient();
    public final String origin;

    public HttpUtil(Javalin javalin) {
        this.origin = "http://localhost:" + javalin.port();
    }

    public Response get(String path) throws Exception {
        return okHttp.newCall(new Request.Builder().url(origin + path).get().build()).execute();
    }

    public String getBody(String path) throws Exception {
        return get(path).body().string();
    }

    public HttpRequestWithBody post(String path) {
        return Unirest.post(origin + path);
    }

}
