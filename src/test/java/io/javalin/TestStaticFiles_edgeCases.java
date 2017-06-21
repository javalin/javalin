/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import java.io.File;

import org.junit.Test;

import io.javalin.embeddedserver.Location;
import io.javalin.event.EventType;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestStaticFiles_edgeCases {

    @Test
    public void test_externalFolder() throws Exception {
        Javalin app = Javalin.create()
            .port(7777)
            .enableStaticFiles("src/test/external/", Location.EXTERNAL)
            .start()
            .awaitInitialization();

        HttpResponse<String> response = Unirest.get("http://localhost:7777/html.html").asString();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), containsString("HTML works"));

        app.stop().awaitTermination();
    }

    @Test
    public void test_nonExistent_classpathFolder() throws Exception {
        String[] message = {""};
        Javalin.create()
            .enableStaticFiles("some-fake-folder")
            .event(EventType.SERVER_START_FAILED, event -> message[0] = "failed")
            .start()
            .awaitInitialization();
        assertThat(message[0], is("failed"));
    }

    @Test
    public void test_nonExistent_externalFolder() throws Exception {
        String[] message = {""};
        Javalin.create()
            .enableStaticFiles("some-fake-folder", Location.EXTERNAL)
            .event(EventType.SERVER_START_FAILED, event -> message[0] = "failed")
            .start()
            .awaitInitialization();
        assertThat(message[0], is("failed"));
    }

    @Test
    public void test_classpathEmptyFolder() throws Exception {
        new File("src/test/external/empty").mkdir();
        String[] message = {""};
        Javalin.create()
            .enableStaticFiles("src/test/external/empty", Location.CLASSPATH)
            .event(EventType.SERVER_START_FAILED, event -> message[0] = "failed")
            .start()
            .awaitInitialization();
        assertThat(message[0], is("failed"));
    }

    @Test
    public void test_externalEmptyFolder() throws Exception {
        new File("src/test/external/empty").mkdir();
        String[] message = {""};
        Javalin.create()
            .enableStaticFiles("src/test/external/empty", Location.EXTERNAL)
            .event(EventType.SERVER_START_FAILED, event -> message[0] = "failed")
            .start()
            .awaitInitialization();
        assertThat(message[0], not("failed"));
    }

}
