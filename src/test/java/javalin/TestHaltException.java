package javalin;

import org.junit.Test;

import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestHaltException extends _UnirestBaseTest {

    @Test
    public void test_haltBeforeWildcard_works() throws Exception {
        app.before("/admin/*", (req, res) -> {
            throw new HaltException(401);
        });
        app.get("/admin/protected", (req, res) -> res.body("Protected resource"));
        HttpResponse<String> response = call(HttpMethod.GET, "/admin/protected");
        assertThat(response.getStatus(), is(401));
        assertThat(response.getBody(), not("Protected resource"));
    }

    @Test
    public void test_haltInRoute_works() throws Exception {
        app.get("/some-route", (req, res) -> {
            throw new HaltException(401, "Stop!");
        });
        HttpResponse<String> response = call(HttpMethod.GET, "/some-route");
        assertThat(response.getBody(), is("Stop!"));
        assertThat(response.getStatus(), is(401));
    }

    @Test
    public void test_afterRuns_afterHalt() throws Exception {
        app.get("/some-route", (req, res) -> {
            throw new HaltException(401, "Stop!");
        }).after((req, res) -> {
            res.status(418);
        });
        HttpResponse<String> response = call(HttpMethod.GET, "/some-route");
        assertThat(response.getBody(), is("Stop!"));
        assertThat(response.getStatus(), is(418));
    }

}
