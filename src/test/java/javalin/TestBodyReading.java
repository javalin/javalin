package javalin;

import org.junit.Test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestBodyReading {

    @Test
    public void test_bodyReader() throws Exception {
        Javalin app = Javalin.create().port(0).start().awaitInitialization();
        app.before("/body-reader", (req, res) -> res.header("X-BEFORE", req.body() + req.queryParam("qp")));
        app.post("/body-reader", (req, res) -> res.body(req.body() + req.queryParam("qp")));
        app.after("/body-reader", (req, res) -> res.header("X-AFTER", req.body() + req.queryParam("qp")));

        HttpResponse<String> response = Unirest
            .post("http://localhost:" + app.port() + "/body-reader")
            .queryString("qp", "queryparam")
            .body("body")
            .asString();

        assertThat(response.getHeaders().getFirst("X-BEFORE"), is("bodyqueryparam"));
        assertThat(response.getBody(), is("bodyqueryparam"));
        assertThat(response.getHeaders().getFirst("X-AFTER"), is("bodyqueryparam"));
        app.stop().awaitTermination();
    }

    @Test
    public void test_bodyReader_reverse() throws Exception {
        Javalin app = Javalin.create().port(0).start().awaitInitialization();
        app.before("/body-reader", (req, res) -> res.header("X-BEFORE", req.queryParam("qp") + req.body()));
        app.post("/body-reader", (req, res) -> res.body(req.queryParam("qp") + req.body()));
        app.after("/body-reader", (req, res) -> res.header("X-AFTER", req.queryParam("qp") + req.body()));

        HttpResponse<String> response = Unirest
            .post("http://localhost:" + app.port() + "/body-reader")
            .queryString("qp", "queryparam")
            .body("body")
            .asString();

        assertThat(response.getHeaders().getFirst("X-BEFORE"), is("queryparambody"));
        assertThat(response.getBody(), is("queryparambody"));
        assertThat(response.getHeaders().getFirst("X-AFTER"), is("queryparambody"));
        app.stop().awaitTermination();
    }

    @Test
    public void test_formParams_work() throws Exception {
        Javalin app = Javalin.create().port(0).start().awaitInitialization();
        app.before("/body-reader", (req, res) -> res.header("X-BEFORE", req.bodyParam("username")));
        app.post("/body-reader", (req, res) -> res.body(req.bodyParam("password")));
        app.after("/body-reader", (req, res) -> res.header("X-AFTER", req.bodyParam("repeat-password")));

        HttpResponse<String> response = Unirest
            .post("http://localhost:" + app.port() + "/body-reader")
            .body("username=some-user-name&password=password&repeat-password=password")
            .asString();

        assertThat(response.getHeaders().getFirst("X-BEFORE"), is("some-user-name"));
        assertThat(response.getBody(), is("password"));
        assertThat(response.getHeaders().getFirst("X-AFTER"), is("password"));
        app.stop().awaitTermination();
    }


}
