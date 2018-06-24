/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

import io.javalin.Handler;
import io.javalin.Javalin;
import io.javalin.PathBuilder;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestPathBuilder {

    private Handler testHandler = ctx -> ctx.result("Hello World");

    @Test
    public void test_noChange_works() {
        assertThat(new PathBuilder("/:path-param").build(), is("/:path-param"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_throwsForMissingColon() {
        new PathBuilder("/:path").pathParam("path", "piñata");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_throwsForBadPathParam() {
        new PathBuilder("/:path").pathParam(":not-path", "piñata");
    }

    @Test
    public void test_singlePathParam_works() {
        String path = new PathBuilder("/:path-param")
            .pathParam(":path-param", "candy-horse")
            .build();
        assertThat(path, is("/candy-horse"));
    }

    @Test
    public void test_singlePathParam_unicode_works() {
        String path = new PathBuilder("/:path-param")
            .pathParam(":path-param", "piñata")
            .build();
        assertThat(path, is("/pi%C3%B1ata"));
    }

    @Test
    public void test_multiplePathParams_works() {
        String path = new PathBuilder("/:path-param/xyz/:path-param2")
            .pathParam(":path-param", "piñata")
            .pathParam(":path-param2", "candy-horse")
            .build();
        assertThat(path, is("/pi%C3%B1ata/xyz/candy-horse"));
    }

    @Test
    public void test_veryMultiplePathParams_works() {
        String path = new PathBuilder("/:1/:2/:3/:11/:22/:33")
            .pathParam(":1", "a")
            .pathParam(":2", "b")
            .pathParam(":3", "c")
            .pathParam(":11", "aa")
            .pathParam(":22", "bb")
            .pathParam(":33", "cc")
            .build();
        assertThat(path, is("/a/b/c/aa/bb/cc"));
    }

    @Test
    public void test_singleQueryParam_works() {
        String path = new PathBuilder("/qp")
            .queryParam("horse-type", "candy")
            .build();
        assertThat(path, is("/qp?horse-type=candy"));
    }

    @Test
    public void test_singleQueryParam_unicode_works() {
        String path = new PathBuilder("/qp")
            .queryParam("horse-type", "piñata")
            .build();
        assertThat(path, is("/qp?horse-type=pi%C3%B1ata"));
    }

    @Test
    public void test_singleQueryParam_multipleValues_works() {
        String path = new PathBuilder("/qp")
            .queryParam("color", "red", "blue", "yellow")
            .build();
        assertThat(path, is("/qp?color=red,blue,yellow"));
    }

    @Test
    public void test_everything_works() {
        String basePath = "/i/have/this/:thing/that/:person/:feeling";
        String path = new PathBuilder(basePath)
            .pathParam(":thing", "piñata")
            .pathParam(":person", "you")
            .pathParam(":feeling", "want")
            .queryParam("size", "large")
            .queryParam("color", "red", "yellow", "blue", "marrón")
            .build();
        assertThat(path, is("/i/have/this/pi%C3%B1ata/that/you/want?size=large&color=red,yellow,blue,marr%C3%B3n"));
    }

    @Test
    public void test_everything_integration_works() {
        Javalin app = Javalin.create().start();
        app.get("/i/have/this/:thing/that/:person/:feeling", testHandler);
        String path = app.pathBuilder(testHandler)
            .pathParam(":thing", "piñata")
            .pathParam(":person", "you")
            .pathParam(":feeling", "want")
            .queryParam("size", "large")
            .queryParam("color", "red", "yellow", "blue", "marrón")
            .build();
        assertThat(path, is("/i/have/this/pi%C3%B1ata/that/you/want?size=large&color=red,yellow,blue,marr%C3%B3n"));
    }

}
