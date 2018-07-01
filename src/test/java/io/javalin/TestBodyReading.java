/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import io.javalin.newutil.TestUtil;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.Test;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestBodyReading {

    @Test
    public void test_bodyReader() {
        new TestUtil().test((app, http) -> {
            app.before("/body-reader", ctx -> ctx.header("X-BEFORE", ctx.body() + ctx.queryParam("qp")));
            app.post("/body-reader", ctx -> ctx.result(ctx.body() + ctx.queryParam("qp")));
            app.after("/body-reader", ctx -> ctx.header("X-AFTER", ctx.body() + ctx.queryParam("qp")));

            HttpResponse<String> response = http.post("/body-reader")
                .queryString("qp", "queryparam")
                .body("body")
                .asString();

            assertThat(response.getHeaders().getFirst("X-BEFORE"), is("bodyqueryparam"));
            assertThat(response.getBody(), is("bodyqueryparam"));
            assertThat(response.getHeaders().getFirst("X-AFTER"), is("bodyqueryparam"));
        });
    }

    @Test
    public void test_bodyReader_reverse() {
        new TestUtil().test((app, http) -> {
            app.before("/body-reader", ctx -> ctx.header("X-BEFORE", ctx.queryParam("qp") + ctx.body()));
            app.post("/body-reader", ctx -> ctx.result(ctx.queryParam("qp") + ctx.body()));
            app.after("/body-reader", ctx -> ctx.header("X-AFTER", ctx.queryParam("qp") + ctx.body()));

            HttpResponse<String> response = http.post("/body-reader")
                .queryString("qp", "queryparam")
                .body("body")
                .asString();

            assertThat(response.getHeaders().getFirst("X-BEFORE"), is("queryparambody"));
            assertThat(response.getBody(), is("queryparambody"));
            assertThat(response.getHeaders().getFirst("X-AFTER"), is("queryparambody"));
        });

    }

    @Test
    public void test_formParams_work() {
        new TestUtil().test((app, http) -> {
            app.before("/body-reader", ctx -> ctx.header("X-BEFORE", ctx.formParam("username")));
            app.post("/body-reader", ctx -> ctx.result(ctx.formParam("password")));
            app.after("/body-reader", ctx -> ctx.header("X-AFTER", ctx.formParam("repeat-password")));

            HttpResponse<String> response = http.post("/body-reader")
                .body("username=some-user-name&password=password&repeat-password=password")
                .asString();

            assertThat(response.getHeaders().getFirst("X-BEFORE"), is("some-user-name"));
            assertThat(response.getBody(), is("password"));
            assertThat(response.getHeaders().getFirst("X-AFTER"), is("password"));
        });
    }

    @Test
    public void test_unicodeFormParams_work() {
        new TestUtil().test((app, http) -> {
            app.post("/unicode", ctx -> ctx.result(ctx.formParam("unicode")));

            String responseBody = http.post("/unicode")
                .body("unicode=♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟")
                .asString().getBody();

            assertThat(responseBody, is("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟"));
        });

    }

    @Test
    public void test_formParamsWork_multipleValues() {
        new TestUtil().test((app, http) -> {
            app.post("/body-reader", ctx -> {
                String formParamString = ctx.formParamMap().keySet().stream()
                    .map(key -> key + ": " + ctx.formParam(key) + ", " + key + "s: " + requireNonNull(ctx.formParams(key)).toString())
                    .collect(Collectors.joining(". "));
                String queryParamString = ctx.queryParamMap().keySet().stream()
                    .map(key -> key + ": " + ctx.queryParam(key) + ", " + key + "s: " + requireNonNull(ctx.queryParams(key)).toString())
                    .collect(Collectors.joining(". "));
                boolean singleMissingSame = Objects.equals(ctx.formParam("missing"), ctx.queryParam("missing"));
                boolean pluralMissingSame = Objects.equals(ctx.formParams("missing"), ctx.queryParams("missing"));
                boolean nonMissingSame = Objects.equals(formParamString, queryParamString);
                if (singleMissingSame && pluralMissingSame && nonMissingSame) {
                    ctx.result(formParamString);
                }
            });

            String params = "a=1&a=2&a=3&b=1&b=2&c=1&d=&e&f=%28%23%29";
            HttpResponse<String> response = http.post("/body-reader?" + params)
                .body(params)
                .asString();

            assertThat(response.getBody(), is("a: 1, as: [1, 2, 3]. b: 1, bs: [1, 2]. c: 1, cs: [1]. d: , ds: []. e: , es: []. f: (#), fs: [(#)]"));
        });
    }

}
