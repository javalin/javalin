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

    public Response get(String path) throws IOException {
        Request request = new Request.Builder()
            .url(origin + path)
            .get()
            .build();
        return okHttp.newCall(request).execute();
    }

    public String getBody(String path) throws IOException {
        return get(path).body().string();
    }

    public Response request(String path, Consumer<Request.Builder> userBuilder) throws IOException {
        Request.Builder finalBuilder = new Request.Builder();
        userBuilder.accept(finalBuilder);
        return okHttp.newCall(finalBuilder.url(origin + path).build()).execute();
    }

}
