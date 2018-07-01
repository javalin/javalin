package io.javalin;

import io.javalin.core.util.Header;
import io.javalin.newutil.TestUtil;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class TestDefaultContentType {

    @Test
    public void test_sane_defaults() {
        new TestUtil().test((app, http) -> {
            app.get("/text", ctx -> ctx.result("суп из капусты"));
            app.get("/json", ctx -> ctx.json("白菜湯"));
            app.get("/html", ctx -> ctx.html("kålsuppe"));
            assertThat(http.get("/text").header(Header.CONTENT_TYPE), is("text/plain"));
            assertThat(http.get("/json").header(Header.CONTENT_TYPE), is("application/json"));
            assertThat(http.get("/html").header(Header.CONTENT_TYPE), is("text/html"));
            assertThat(http.getBody("/text"), is("суп из капусты"));
            assertThat(http.getBody("/json"), is("\"白菜湯\""));
            assertThat(http.getBody("/html"), is("kålsuppe"));
        });
    }

    @Test
    public void test_sets_default() {
        new TestUtil(Javalin.create().defaultContentType("application/json")).test((app, http) -> {
            app.get("/default", ctx -> ctx.result("not json"));
            assertThat(http.get("/default").header(Header.CONTENT_TYPE), containsString("application/json"));
        });

    }

    @Test
    public void test_allows_overrides() {
        new TestUtil(Javalin.create().defaultContentType("application/json")).test((app, http) -> {
            app.get("/override", ctx -> {
                ctx.res.setCharacterEncoding("utf-8");
                ctx.res.setContentType("text/html");
            });
            assertThat(http.get("/override").header(Header.CONTENT_TYPE), containsString("utf-8"));
            assertThat(http.get("/override").header(Header.CONTENT_TYPE), containsString("text/html"));
        });
    }

}
