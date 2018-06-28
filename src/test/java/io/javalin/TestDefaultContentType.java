package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.javalin.core.util.Header;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class TestDefaultContentType {

    private HttpResponse<String> get(Javalin app, String path) throws UnirestException {
        return Unirest.get("http://localhost:" + app.port() + path).asString();
    }

    @Test
    public void test_sane_defaults() throws Exception {
        Javalin app = Javalin.create().start(0);
        app.get("/text", ctx -> ctx.result("суп из капусты"));
        app.get("/json", ctx -> ctx.json("白菜湯"));
        app.get("/html", ctx -> ctx.html("kålsuppe"));
        assertThat(get(app, "/text").getHeaders().getFirst(Header.CONTENT_TYPE), is("text/plain"));
        assertThat(get(app, "/json").getHeaders().getFirst(Header.CONTENT_TYPE), is("application/json"));
        assertThat(get(app, "/html").getHeaders().getFirst(Header.CONTENT_TYPE), is("text/html"));
        assertThat(get(app, "/text").getBody(), is("суп из капусты"));
        assertThat(get(app, "/json").getBody(), is("\"白菜湯\""));
        assertThat(get(app, "/html").getBody(), is("kålsuppe"));
        app.stop();
    }

    @Test
    public void test_sets_default() throws Exception {
        Javalin app = Javalin.create().defaultContentType("application/json").start(0);
        app.get("/default", ctx -> ctx.result("not json"));
        assertThat(get(app, "/default").getHeaders().getFirst(Header.CONTENT_TYPE), containsString("application/json"));
        app.stop();
    }

    @Test
    public void test_allows_overrides() throws Exception {
        Javalin app = Javalin.create().defaultContentType("application/json").start(0);
        app.get("/override", ctx -> {
            ctx.response().setCharacterEncoding("utf-8");
            ctx.response().setContentType("text/html");
            ctx.result("mmm");
        });
        String contentType = get(app, "/override").getHeaders().getFirst(Header.CONTENT_TYPE);
        assertThat(contentType, containsString("utf-8"));
        assertThat(contentType, containsString("text/html"));
        app.stop();
    }

}
