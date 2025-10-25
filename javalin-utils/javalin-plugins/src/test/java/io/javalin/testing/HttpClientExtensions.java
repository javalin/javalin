package io.javalin.testing;

import io.javalin.testtools.HttpClient;
import io.javalin.testtools.Response;

public class HttpClientExtensions {

    public static String getBody(HttpClient client, String path) {
        // Follow redirects manually (up to 10 redirects)
        Response response = client.get(path);
        int redirectCount = 0;
        while (response.code() >= 300 && response.code() < 400 && redirectCount < 10) {
            String location = response.headers().get("Location").get(0);
            response = client.get(location);
            redirectCount++;
        }
        return response.body().string();
    }
}

