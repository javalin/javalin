/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestBodyReading {

    @Test
    public void test_bodyReader() throws Exception {
        Javalin app = Javalin.create().port(0).start();
        app.before("/body-reader", ctx -> ctx.header("X-BEFORE", ctx.body() + ctx.queryParam("qp")));
        app.post("/body-reader", ctx -> ctx.result(ctx.body() + ctx.queryParam("qp")));
        app.after("/body-reader", ctx -> ctx.header("X-AFTER", ctx.body() + ctx.queryParam("qp")));

        HttpResponse<String> response = Unirest
            .post("http://localhost:" + app.port() + "/body-reader")
            .queryString("qp", "queryparam")
            .body("body")
            .asString();

        assertThat(response.getHeaders().getFirst("X-BEFORE"), is("bodyqueryparam"));
        assertThat(response.getBody(), is("bodyqueryparam"));
        assertThat(response.getHeaders().getFirst("X-AFTER"), is("bodyqueryparam"));
        app.stop();
    }

    @Test
    public void test_bodyReader_reverse() throws Exception {
        Javalin app = Javalin.create().port(0).start();
        app.before("/body-reader", ctx -> ctx.header("X-BEFORE", ctx.queryParam("qp") + ctx.body()));
        app.post("/body-reader", ctx -> ctx.result(ctx.queryParam("qp") + ctx.body()));
        app.after("/body-reader", ctx -> ctx.header("X-AFTER", ctx.queryParam("qp") + ctx.body()));

        HttpResponse<String> response = Unirest
            .post("http://localhost:" + app.port() + "/body-reader")
            .queryString("qp", "queryparam")
            .body("body")
            .asString();

        assertThat(response.getHeaders().getFirst("X-BEFORE"), is("queryparambody"));
        assertThat(response.getBody(), is("queryparambody"));
        assertThat(response.getHeaders().getFirst("X-AFTER"), is("queryparambody"));
        app.stop();
    }

    @Test
    public void test_formParams_work() throws Exception {
        Javalin app = Javalin.create().port(0).start();
        app.before("/body-reader", ctx -> ctx.header("X-BEFORE", ctx.formParam("username")));
        app.post("/body-reader", ctx -> ctx.result(ctx.formParam("password")));
        app.after("/body-reader", ctx -> ctx.header("X-AFTER", ctx.formParam("repeat-password")));

        HttpResponse<String> response = Unirest
            .post("http://localhost:" + app.port() + "/body-reader")
            .body("username=some-user-name&password=password&repeat-password=password")
            .asString();

        assertThat(response.getHeaders().getFirst("X-BEFORE"), is("some-user-name"));
        assertThat(response.getBody(), is("password"));
        assertThat(response.getHeaders().getFirst("X-AFTER"), is("password"));
        app.stop();
    }

    @Test
    public void test_formParamsWork_multipleValues() throws Exception {
        Javalin app = Javalin.create().port(0).start();
        app.post("/body-reader", ctx -> {
            String formParamString = ctx.formParamMap().keySet().stream().map(key -> {
                return key + ": " + ctx.formParam(key) + ", " + key + "s: " + Arrays.toString(ctx.formParams(key));
            }).collect(Collectors.joining(". "));
            String queryParamString = ctx.queryParamMap().keySet().stream().map(key -> {
                return key + ": " + ctx.queryParam(key) + ", " + key + "s: " + Arrays.toString(ctx.queryParams(key));
            }).collect(Collectors.joining(". "));
            boolean singleMissingSame = Objects.equals(ctx.formParam("missing"), ctx.queryParam("missing"));
            boolean pluralMissingSame = Arrays.equals(ctx.formParams("missing"), ctx.queryParams("missing"));
            boolean nonMissingSame = Objects.equals(formParamString, queryParamString);
            if (singleMissingSame && pluralMissingSame && nonMissingSame) {
                ctx.result(formParamString);
            }
        });

        String params = "a=1&a=2&a=3&b=1&b=2&c=1&d=&e&f=%28%23%29";
        HttpResponse<String> response = Unirest
            .post("http://localhost:" + app.port() + "/body-reader?" + params)
            .body(params)
            .asString();

        assertThat(response.getBody(), is("a: 1, as: [1, 2, 3]. b: 1, bs: [1, 2]. c: 1, cs: [1]. d: , ds: []. e: , es: []. f: (#), fs: [(#)]"));
        app.stop();
    }


}
