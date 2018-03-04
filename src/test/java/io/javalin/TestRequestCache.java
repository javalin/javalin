package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;

public class TestRequestCache {

    private static Javalin appCacheEnabled;
    private static Javalin appCacheDisabled;
    private static String originCacheEnabled;
    private static String originCacheDisabled;

    @BeforeClass
    public static void setUp() {
        appCacheEnabled = Javalin.start(0);
        originCacheEnabled = "http://localhost:" + appCacheEnabled.port();

        appCacheDisabled = Javalin.create()
            .port(0)
            .disableRequestCache()
            .start();
        originCacheDisabled = "http://localhost:" + appCacheDisabled.port();
    }

    @AfterClass
    public static void tearDown() {
        appCacheEnabled.stop();
        appCacheDisabled.stop();
    }

    @Test
    public void test_cache_not_draining_InputStream() throws Exception {
        appCacheEnabled.post("/cache-chunked-encoding", ctx -> ctx.result(ctx.request().getInputStream()));

        byte[] body = new byte[10000];
        for (int i = 0; i < body.length; i++) {
            body[i] = (byte) (i % 256);
        }

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(originCacheEnabled + "/cache-chunked-encoding");
        ByteArrayEntity entity = new ByteArrayEntity(body);
        entity.setChunked(true);
        post.setEntity(entity);
        CloseableHttpResponse response = client.execute(post);
        byte[] result = EntityUtils.toByteArray(response.getEntity());

        assertThat("Body should match", Arrays.equals(result, body));

        response.close();
    }

    @Test
    public void test_allows_disabling_cache() throws Exception {
        appCacheDisabled.post("/disabled-cache", ctx -> {
            if (ctx.request().getInputStream().getClass().getSimpleName().equals("CachedServletInputStream")) {
                throw new IllegalStateException("Cache should be disabled");
            } else {
                ctx.result("");
            }
        });
        HttpResponse<InputStream> response = Unirest.post(originCacheDisabled + "/disabled-cache")
                .body("test")
                .asBinary();

        assertThat("Request cache should be disabled", response.getStatus() == 200);
    }
}
