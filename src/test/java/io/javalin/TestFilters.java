/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import org.junit.Test;

import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestFilters extends _UnirestBaseTest {

    @Test
    public void test_justFilters_is404() throws Exception {
        Handler emptyHandler = ctx -> {
        };
        app.before(emptyHandler);
        app.after(emptyHandler);
        HttpResponse<String> response = call(HttpMethod.GET, "/hello");
        assertThat(response.getStatus(), is(404));
        assertThat(response.getBody(), is("Not found"));
    }

    @Test
    public void test_beforeFilter_setsHeader() throws Exception {
        app.before(ctx -> ctx.header("X-FILTER", "Before-filter ran"));
        app.get("/mapped", OK_HANDLER);
        assertThat(getAndGetHeader("/mapped", "X-FILTER"), is("Before-filter ran"));
    }

    @Test
    public void test_multipleFilters_setHeaders() throws Exception {
        app.before(ctx -> ctx.header("X-FILTER-1", "Before-filter 1 ran"));
        app.before(ctx -> ctx.header("X-FILTER-2", "Before-filter 2 ran"));
        app.before(ctx -> ctx.header("X-FILTER-3", "Before-filter 3 ran"));
        app.after(ctx -> ctx.header("X-FILTER-4", "After-filter 1 ran"));
        app.after(ctx -> ctx.header("X-FILTER-5", "After-filter 2 ran"));
        app.get("/mapped", OK_HANDLER);
        assertThat(getAndGetHeader("/mapped", "X-FILTER-1"), is("Before-filter 1 ran"));
        assertThat(getAndGetHeader("/mapped", "X-FILTER-2"), is("Before-filter 2 ran"));
        assertThat(getAndGetHeader("/mapped", "X-FILTER-3"), is("Before-filter 3 ran"));
        assertThat(getAndGetHeader("/mapped", "X-FILTER-4"), is("After-filter 1 ran"));
        assertThat(getAndGetHeader("/mapped", "X-FILTER-5"), is("After-filter 2 ran"));
    }

    @Test
    public void test_afterFilter_setsHeader() throws Exception {
        app.after(ctx -> ctx.header("X-FILTER", "After-filter ran"));
        app.get("/mapped", OK_HANDLER);
        assertThat(getAndGetHeader("/mapped", "X-FILTER"), is("After-filter ran"));
    }

    @Test
    public void test_afterFilter_overrides_beforeFilter() throws Exception {
        app.before(ctx -> ctx.header("X-FILTER", "This header is mine!"));
        app.after(ctx -> ctx.header("X-FILTER", "After-filter beats before-filter"));
        app.get("/mapped", OK_HANDLER);
        assertThat(getAndGetHeader("/mapped", "X-FILTER"), is("After-filter beats before-filter"));
    }

    @Test
    public void test_beforeFilter_canAddTrailingSlashes() throws Exception {
        app.before(ctx -> {
            if (!ctx.path().endsWith("/")) {
                ctx.redirect(ctx.path() + "/");
            }
        });
        app.get("/ok/", OK_HANDLER);
        assertThat(GET_body("/ok"), is("OK"));
    }

    private String getAndGetHeader(String path, String header) throws UnirestException {
        return call(HttpMethod.GET, path).getHeaders().getFirst(header);
    }

}
