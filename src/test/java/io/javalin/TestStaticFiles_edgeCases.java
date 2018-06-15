/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.javalin.staticfiles.Location;
import java.io.File;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class TestStaticFiles_edgeCases {

    @Test
    public void test_externalFolder() throws Exception {
        Javalin app = Javalin.create()
            .port(0)
            .enableStaticFiles("src/test/external/", Location.EXTERNAL)
            .start();

        HttpResponse<String> response = Unirest.get("http://localhost:" + app.port() + "/html.html").asString();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), containsString("HTML works"));

        app.stop();
    }

    @Test
    public void test_nonExistent_classpathFolder() {
        String[] message = {""};
        Javalin.create()
            .port(0)
            .enableStaticFiles("some-fake-folder")
            .event(JavalinEvent.SERVER_START_FAILED, () -> message[0] = "failed")
            .start()
            .stop();
        assertThat(message[0], is("failed"));
    }

    @Test
    public void test_nonExistent_externalFolder() {
        String[] message = {""};
        Javalin.create()
            .port(0)
            .enableStaticFiles("some-fake-folder", Location.EXTERNAL)
            .event(JavalinEvent.SERVER_START_FAILED, () -> message[0] = "failed")
            .start()
            .stop();
        assertThat(message[0], is("failed"));
    }

    @Test
    public void test_classpathEmptyFolder() {
        new File("src/test/external/empty").mkdir();
        String[] message = {""};
        Javalin.create()
            .port(0)
            .enableStaticFiles("src/test/external/empty", Location.CLASSPATH)
            .event(JavalinEvent.SERVER_START_FAILED, () -> message[0] = "failed")
            .start()
            .stop();
        assertThat(message[0], is("failed"));
    }

    @Test
    public void test_externalEmptyFolder() {
        new File("src/test/external/empty").mkdir();
        String[] message = {""};
        Javalin.create()
            .port(0)
            .enableStaticFiles("src/test/external/empty", Location.EXTERNAL)
            .event(JavalinEvent.SERVER_START_FAILED, () -> message[0] = "failed")
            .start()
            .stop();
        assertThat(message[0], not("failed"));
    }

}
