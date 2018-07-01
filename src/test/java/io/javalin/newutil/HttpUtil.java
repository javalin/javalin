/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.newutil;

import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;
import io.javalin.Javalin;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.impl.client.HttpClients;

public class HttpUtil {

    private final OkHttpClient okHttp = new OkHttpClient();
    public final String origin;

    public HttpUtil(Javalin javalin) {
        this.origin = "http://localhost:" + javalin.port();
    }

    public void enableUnirestRedirects() {
        Unirest.setHttpClient(HttpClients.custom().build());
    }

    public void disableUnirestRedirects() {
        Unirest.setHttpClient(HttpClients.custom().disableRedirectHandling().build());
    }

    // OkHTTP

    public Response get(String path) throws Exception {
        return okHttp.newCall(new Request.Builder().url(origin + path).get().build()).execute();
    }

    public String getBody(String path) throws Exception {
        return get(path).body().string();
    }

    // Unirest

    public HttpResponse<String> call(HttpMethod method, String pathname) throws UnirestException {
        return new HttpRequestWithBody(method, origin + pathname).asString();
    }

    public HttpRequestWithBody post(String path) {
        return Unirest.post(origin + path);
    }

    public String getBody_withCookies(String path) throws Exception { // OkHttp makes using cookies very hard
        return Unirest.get(origin + path).asString().getBody();
    }

    public HttpResponse<String> get_withCookies(String path) throws Exception { // OkHttp makes using cookies very hard
        return Unirest.get(origin + path).asString();
    }

}
