/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.javalin.core.util.Header;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class TestGzip {

    private static OkHttpClient okHttp = new OkHttpClient();
    private static Javalin app;
    private static String origin = null;
    private static int tinyLength = SillyObject.getSomeObjects(10).toString().length();
    private static int hugeLength = SillyObject.getSomeObjects(1000).toString().length();

    @BeforeClass
    public static void setup() {
        app = Javalin.create()
            .port(0)
            .start();
        app.get("/huge", ctx -> ctx.result(SillyObject.getSomeObjects(1000).toString()));
        app.get("/tiny", ctx -> ctx.result(SillyObject.getSomeObjects(10).toString()));
        origin = "http://localhost:" + app.port();
    }

    @AfterClass
    public static void tearDown() {
        app.stop();
    }

    @Test
    public void test_doesNotZip_whenAcceptsIsNotSet() throws Exception {
        HttpResponse<String> unzipped = Unirest.get(origin + "/huge").header(Header.ACCEPT_ENCODING, "null").asString();
        assertThat(unzipped.getBody().length(), is(hugeLength));
        assertThat(getResponse("/huge", "null").headers().get(Header.CONTENT_ENCODING), is(nullValue()));
    }

    @Test
    public void test_doesNotZip_whenSizeIsTiny() throws Exception {
        HttpResponse<String> unzipped = Unirest.get(origin + "/tiny").asString();
        assertThat(unzipped.getBody().length(), is(tinyLength));
        assertThat(getResponse("/tiny", "gzip").headers().get(Header.CONTENT_ENCODING), is(nullValue()));
    }

    @Test
    public void test_doesNotZip_whenSizeIsHuge_andAcceptsIsNotSet() throws Exception {
        HttpResponse<String> unzipped = Unirest.get(origin + "/huge").header(Header.ACCEPT_ENCODING, "null").asString();
        assertThat(unzipped.getBody().length(), is(hugeLength));
        assertThat(getResponse("/huge", "null").headers().get(Header.CONTENT_ENCODING), is(nullValue()));
    }

    @Test
    public void test_doesZip_whenSizeIsHuge_andAcceptsIsSet() throws Exception {
        HttpResponse<String> zipped = Unirest.get(origin + "/huge").asString();
        assertThat(zipped.getBody().length(), is(hugeLength));
        assertThat(getResponse("/huge", "gzip").headers().get(Header.CONTENT_ENCODING), is("gzip"));
        assertThat(getResponse("/huge", "gzip").body().contentLength(), is(7717L)); // hardcoded because lazy
    }

    @Test
    public void test_doesNotZip_whenGzipDisabled() throws Exception {
        Javalin noGzipApp = Javalin.create()
            .port(0)
            .disableDynamicGzip()
            .get("/huge", ctx -> ctx.result(SillyObject.getSomeObjects(1000).toString()))
            .start();
        Response response = okHttp.newCall(new Request.Builder()
            .url("http://localhost:" + noGzipApp.port() + "/huge")
            .header(Header.ACCEPT_ENCODING, "gzip")
            .build()).execute();
        assertThat(response.headers().get(Header.CONTENT_ENCODING), is(nullValue()));
        noGzipApp.stop();
    }

    // we need to use okhttp, because unirest omits the content-encoding header
    private Response getResponse(String url, String encoding) throws IOException {
        return okHttp.newCall(new Request.Builder()
            .url(origin + url)
            .header(Header.ACCEPT_ENCODING, encoding)
            .build()).execute();
    }

    static class SillyObject {
        public String fieldOne;
        public String fieldTwo;
        public String fieldThree;

        public SillyObject(String fieldOne, String fieldTwo, String fieldThree) {
            this.fieldOne = fieldOne;
            this.fieldTwo = fieldTwo;
            this.fieldThree = fieldThree;
        }

        public String toString() {
            return "SillyObject{" +
                "fieldOne='" + fieldOne + '\'' +
                ", fieldTwo='" + fieldTwo + '\'' +
                ", fieldThree='" + fieldThree + '\'' +
                '}';
        }

        static List<SillyObject> getSomeObjects(int numberOfObjects) {
            return IntStream.range(0, numberOfObjects)
                .mapToObj(i -> new SillyObject("f" + i, "f" + i, "f" + i))
                .collect(Collectors.toList());
        }
    }

}
