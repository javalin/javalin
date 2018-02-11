package io.javalin;

import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.request.HttpRequestWithBody;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class TestDefaultContentType {

    private static Javalin app;
    private static String origin;

    @BeforeClass
    public static void setUp() {
        app = Javalin.create()
            .defaultCharacterEncoding("windows-1251")
            .defaultContentType("application/json")
            .start();
        origin = "http://localhost:" + app.port();
    }

    @Test
    public void test_allows_ISO_8859_1() throws Exception {
        app.get("/test-iso-encoding", ctx -> {
            ctx.response().setCharacterEncoding("iso-8859-1");
            ctx.result("");
        });

        HttpResponse<String> response = new HttpRequestWithBody(HttpMethod.GET, origin + "/test-iso-encoding").asString();
        String charset = response.getHeaders().getFirst("Content-Type");
        assertThat(charset, containsString("iso-8859-1"));
    }

    @Test
    public void test_sets_defaults() throws Exception {
        app.get("/test-default-encoding", ctx -> ctx.result(""));

        HttpResponse<String> response = new HttpRequestWithBody(HttpMethod.GET, origin + "/test-default-encoding").asString();
        String charset = response.getHeaders().getFirst("Content-Type");
        assertThat(charset, containsString("windows-1251"));
        assertThat(charset, containsString("application/json"));
    }

    @Test
    public void test_allows_overrides() throws Exception {
        app.get("/test-override-encoding", ctx -> {
            ctx.response().setCharacterEncoding("utf-8");
            ctx.response().setContentType("text/html");
            ctx.result("");
        });

        HttpResponse<String> response = new HttpRequestWithBody(HttpMethod.GET, origin + "/test-override-encoding").asString();
        String charset = response.getHeaders().getFirst("Content-Type");
        assertThat(charset, containsString("utf-8"));
        assertThat(charset, containsString("text/html"));
    }

    @AfterClass
    public static void tearDown() {
        app.stop();
    }
}
