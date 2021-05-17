package io.javalin.testtools;

import java.io.IOException;
import java.util.function.Consumer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpClient {

    OkHttpClient okHttp = new OkHttpClient();
    String origin;

    public HttpClient(int port) {
        this.origin = "http://localhost:" + port;
    }

    public Response request(Request request) throws IOException {
        return okHttp.newCall(request).execute();
    }

    public Response request(String path, Consumer<Request.Builder> userBuilder) throws IOException {
        Request.Builder finalBuilder = new Request.Builder();
        userBuilder.accept(finalBuilder);
        return request(finalBuilder.url(origin + path).build());
    }

    public Response get(String path) throws IOException {
        return request(path, Request.Builder::get);
    }

    public String getBody(String path) throws IOException {
        return get(path).body().string();
    }



}
