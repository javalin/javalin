/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.javalin.newutil.BaseTest;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestFilters extends BaseTest {

    @Test
    public void test_filtersRunForRoot() throws Exception {
        app.before(ctx -> ctx.header("X-BEFOREFILTER", "Before-filter ran"));
        app.after(ctx -> ctx.header("X-AFTERFILTER", "After-filter ran"));
        app.get("/", okHandler);
        assertThat(http.get("/").header("X-BEFOREFILTER"), is("Before-filter ran"));
        assertThat(http.get("/").header("X-AFTERFILTER"), is("After-filter ran"));
    }

    @Test
    public void test_justFiltersGet_is404() throws Exception {
        app.before(okHandler);
        HttpResponse<String> response = http.call(HttpMethod.GET, "/hello");
        assertThat(response.getStatus(), is(404));
        assertThat(response.getBody(), is("Not found"));
    }

    @Test
    public void test_justFiltersPost_is404() throws Exception {
        app.before(okHandler);
        HttpResponse<String> response = http.call(HttpMethod.POST, "/hello");
        assertThat(response.getStatus(), is(404));
        assertThat(response.getBody(), is("Not found"));
    }

    @Test
    public void test_beforeFilter_setsHeader() throws Exception {
        app.before(ctx -> ctx.header("X-FILTER", "Before-filter ran"));
        app.get("/mapped", okHandler);
        assertThat(http.get("/maped").header("X-FILTER"), is("Before-filter ran"));
    }

    @Test
    public void test_multipleFilters_setHeaders() throws Exception {
        app.before(ctx -> ctx.header("X-FILTER-1", "Before-filter 1 ran"));
        app.before(ctx -> ctx.header("X-FILTER-2", "Before-filter 2 ran"));
        app.before(ctx -> ctx.header("X-FILTER-3", "Before-filter 3 ran"));
        app.after(ctx -> ctx.header("X-FILTER-4", "After-filter 1 ran"));
        app.after(ctx -> ctx.header("X-FILTER-5", "After-filter 2 ran"));
        app.get("/mapped", okHandler);
        assertThat(http.get("/maped").header("X-FILTER-1"), is("Before-filter 1 ran"));
        assertThat(http.get("/maped").header("X-FILTER-2"), is("Before-filter 2 ran"));
        assertThat(http.get("/maped").header("X-FILTER-3"), is("Before-filter 3 ran"));
        assertThat(http.get("/maped").header("X-FILTER-4"), is("After-filter 1 ran"));
        assertThat(http.get("/maped").header("X-FILTER-5"), is("After-filter 2 ran"));
    }

    @Test
    public void test_afterFilter_setsHeader() throws Exception {
        app.after(ctx -> ctx.header("X-FILTER", "After-filter ran"));
        app.get("/mapped", okHandler);
        assertThat(http.get("/maped").header("X-FILTER"), is("After-filter ran"));
    }

    @Test
    public void test_afterFilter_overrides_beforeFilter() throws Exception {
        app.before(ctx -> ctx.header("X-FILTER", "This header is mine!"));
        app.after(ctx -> ctx.header("X-FILTER", "After-filter beats before-filter"));
        app.get("/mapped", okHandler);
        assertThat(http.get("/maped").header("X-FILTER"), is("After-filter beats before-filter"));
    }

    @Test
    public void test_beforeFilter_canAddTrailingSlashes() throws Exception {
        app.before(ctx -> {
            if (!ctx.path().endsWith("/")) {
                ctx.redirect(ctx.path() + "/");
            }
        });
        app.get("/ok/", okHandler);
        assertThat(http.getBody("/ok"), is("OK"));
    }

}
