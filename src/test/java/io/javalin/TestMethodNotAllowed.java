package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import javax.servlet.http.HttpServletResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestMethodNotAllowed {

    private static final String EXPECTED_HTML_BODY = ""
        + "<!DOCTYPE html>\n"
        + "<html lang=\"en\">\n"
        + "    <head>\n"
        + "        <meta charset=\"UTF-8\">\n"
        + "        <title>Method Not Allowed</title>\n"
        + "    </head>\n"
        + "    <body>\n"
        + "        <h1>405 - Method Not Allowed</h1>\n"
        + "        <p>\n"
        + "            Available Methods: <strong>GET, PUT, DELETE</strong>\n"
        + "        </p>\n"
        + "    </body>\n"
        + "</html>";
    private static final String EXPECTED_JSON_BODY = "{\"availableMethods\":[\"GET\", \"PUT\", \"DELETE\"]}";
    private static Javalin app;
    private static String baseUrl;

    @BeforeClass
    public static void setup() {
        app = Javalin.create()
            .port(0)
            .prefer405over404()
            .start();
        baseUrl = "http://localhost:" + app.port();

        app.get("/test", ctx -> ctx.result("Hello world"));
        app.put("/test", ctx -> ctx.result("Hello world"));
        app.delete("/test", ctx -> ctx.result("Hello world"));
    }

    @Test
    public void test_htmlMethodNotAllowed() throws UnirestException {
        HttpResponse<String> response = Unirest.post(baseUrl + "/test").header("Accept", "text/html").asString();
        assertThat(response.getStatus(), is(HttpServletResponse.SC_METHOD_NOT_ALLOWED));
        assertThat(response.getBody(), is(EXPECTED_HTML_BODY));
    }

    @Test
    public void test_jsonMethodNotAllowed() throws UnirestException {
        HttpResponse<String> response = Unirest.post(baseUrl + "/test").asString();
        assertThat(response.getStatus(), is(HttpServletResponse.SC_METHOD_NOT_ALLOWED));
        assertThat(response.getBody(), is(EXPECTED_JSON_BODY));
    }

    @AfterClass
    public static void tearDown() {
        app.stop();
    }
}
