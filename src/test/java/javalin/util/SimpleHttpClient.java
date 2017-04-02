package javalin.util;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

public class SimpleHttpClient {

    private HttpClient httpClient;

    public SimpleHttpClient() {
        this.httpClient = httpClientBuilder().build();
    }

    private HttpClientBuilder httpClientBuilder() {
        return HttpClientBuilder.create().setConnectionManager(
            new BasicHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .build()
            )
        );
    }

    public TestResponse http_GET(String path) throws IOException {
        HttpResponse httpResponse = httpClient.execute(new HttpGet(path));
        HttpEntity entity = httpResponse.getEntity();
        return new TestResponse(
            EntityUtils.toString(entity),
            httpResponse.getStatusLine().getStatusCode()
        );
    }

    public static class TestResponse {
        public String body;
        public int status;

        private TestResponse(String body, int status) {
            this.body = body;
            this.status = status;
        }
    }

}
