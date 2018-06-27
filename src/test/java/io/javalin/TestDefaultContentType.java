package io.javalin;

import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.HttpRequestWithBody;
import io.javalin.core.util.Header;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class TestDefaultContentType {

    private static Javalin app;
    private static String origin;

    @BeforeClass
    public static void setUp() {
        app = Javalin.create()
            .port(0)
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
        String contentType = Unirest.get(origin + "/test-iso-encoding").asString().getHeaders().getFirst(Header.CONTENT_TYPE);
        assertThat(contentType, containsString("iso-8859-1"));
    }

    @Test
    public void test_sets_default() throws Exception {
        app.get("/test-default-encoding", ctx -> ctx.result(""));
        String contentType = Unirest.get(origin + "/test-default-encoding").asString().getHeaders().getFirst(Header.CONTENT_TYPE);
        assertThat(contentType, containsString("application/json"));
    }

    @Test
    public void test_sane_defaults() throws Exception {
        Javalin app = Javalin.create().start(0)
            .get("/json", ctx -> ctx.json("白菜湯"))
            .get("/html", ctx -> ctx.html("kålsuppe"))
            .get("/text", ctx -> ctx.result("щи"));
        HttpResponse<String> jsonResponse = Unirest.get("http://localhost:" + app.port() + "/json").asString();
        HttpResponse<String> htmlResponse = Unirest.get("http://localhost:" + app.port() + "/html").asString();
        HttpResponse<String> textResponse = Unirest.get("http://localhost:" + app.port() + "/text").asString();
        assertThat(jsonResponse.getHeaders().getFirst(Header.CONTENT_TYPE), is("application/json"));
        assertThat(htmlResponse.getHeaders().getFirst(Header.CONTENT_TYPE), is("text/html"));
        assertThat(textResponse.getHeaders().getFirst(Header.CONTENT_TYPE), is("text/plain"));
        assertThat(jsonResponse.getBody(), is("\"白菜湯\""));
        assertThat(htmlResponse.getBody(), is("kålsuppe"));
        assertThat(textResponse.getBody(), is("щи"));
    }

    @Test
    public void test_allows_overrides() throws Exception {
        app.get("/test-override-encoding", ctx -> {
            ctx.response().setCharacterEncoding("utf-8");
            ctx.response().setContentType("text/html");
            ctx.result("");
        });
        String contentType = Unirest.get(origin + "/test-override-encoding").asString().getHeaders().getFirst(Header.CONTENT_TYPE);
        assertThat(contentType, containsString("utf-8"));
        assertThat(contentType, containsString("text/html"));
    }

    @AfterClass
    public static void tearDown() {
        app.stop();
    }
}
