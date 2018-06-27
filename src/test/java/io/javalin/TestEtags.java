/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.javalin.core.util.Header;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;

public class TestEtags {

    @Test
    public void test_etags_work() throws Exception {
        Javalin app = Javalin.create().enableDynamicEtags().start(0);
        app.get("/", ctx -> ctx.result("Hello!"));
        HttpResponse<String> response = Unirest.get("http://localhost:" + app.port() + "/").asString();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), is("Hello!"));
        String etag = response.getHeaders().getFirst(Header.ETAG);
        HttpResponse<String> response2 = Unirest.get("http://localhost:" + app.port() + "/").header(Header.IF_NONE_MATCH, etag).asString();
        assertThat(response2.getStatus(), is(304));
        assertThat(response2.getBody(), isEmptyOrNullString());
        app.stop();
    }

    @Test
    public void test_no_etags_work() throws Exception {
        Javalin app = Javalin.create().start(0);
        app.get("/", ctx -> ctx.result("Hello!"));
        HttpResponse<String> response = Unirest.get("http://localhost:" + app.port() + "/").asString();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), is("Hello!"));
        String etag = response.getHeaders().getFirst(Header.ETAG);
        assertThat(etag, isEmptyOrNullString());
        app.stop();
    }

}
