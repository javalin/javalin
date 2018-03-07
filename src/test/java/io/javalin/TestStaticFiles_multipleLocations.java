/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.javalin.embeddedserver.Location;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;


public class TestStaticFiles_multipleLocations {

    @Test
    public void test_multipleCallsToEnableStaticFiles() throws Exception {
        Javalin app = Javalin.create()
            .port(0)
            .enableStaticFiles("src/test/external/", Location.EXTERNAL)
            .enableStaticFiles("/public/immutable")
            .enableStaticFiles("/public/protected")
            .enableStaticFiles("/public/subdir")
            .start();
        String origin = "http://localhost:" + app.port();

        HttpResponse<String> response = Unirest.get(origin + "/html.html").asString(); // src/test/external/html.html
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), containsString("HTML works"));
        HttpResponse<String> response2 = Unirest.get(origin + "/").asString();
        assertThat(response2.getStatus(), is(200));
        assertThat(response2.getBody(), is("<h1>Welcome file</h1>"));
        HttpResponse<String> response3 = Unirest.get(origin + "/secret.html").asString();
        assertThat(response3.getStatus(), is(200));
        assertThat(response3.getBody(), is("<h1>Secret file</h1>"));
        HttpResponse<String> response4 = Unirest.get(origin + "/styles.css").asString();
        assertThat(response4.getStatus(), is(404));

        app.stop();
    }

}
