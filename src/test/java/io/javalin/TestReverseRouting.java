/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.core.HandlerType;
import org.junit.Test;
import static io.javalin.security.Role.roles;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TestReverseRouting extends _UnirestBaseTest {

    private Handler helloHandler = ctx -> ctx.result("Hello World");

    @Test
    public void test_pathFinder_works_field() {
        app.get("/hello-get", helloHandler);
        assertThat(app.pathBuilder(helloHandler).build(), is("/hello-get"));
    }

    @Test
    public void test_pathFinder_works_methodRef() {
        app.get("/hello-get", SomeController::methodRef);
        assertThat(app.pathBuilder(SomeController::methodRef).build(), is("/hello-get"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_pathFinder_throws_methodRef_sameBody() {
        app.get("/hello-get", SomeController::methodRef);
        assertThat(app.pathBuilder(SomeController::methodRef2).build(), is("/hello-get"));
    }

    @Test
    public void test_pathFinder_works_implementingClass() {
        ImplementingClass implementingClass = new ImplementingClass();
        app.get("/hello-get", implementingClass);
        assertThat(app.pathBuilder(implementingClass).build(), is("/hello-get"));
    }

    @Test
    public void test_pathFinder_works_accessManager() {
        app.get("/hello-get", helloHandler, roles(TestAccessManager.MyRoles.ROLE_ONE));
        assertThat(app.pathBuilder(helloHandler).build(), is("/hello-get"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_pathFinder_throwsForUnmappedHandler() {
        assertThat(app.pathBuilder(helloHandler).build(), is(nullValue()));
    }

    @Test
    public void test_pathFinder_works_typed() {
        app.get("/hello-get", helloHandler);
        app.post("/hello-post", helloHandler);
        app.before("/hello-post", helloHandler);
        assertThat(app.pathBuilder(helloHandler).build(), is("/hello-get"));
        assertThat(app.pathBuilder(helloHandler, HandlerType.POST).build(), is("/hello-post"));
        assertThat(app.pathBuilder(helloHandler, HandlerType.BEFORE).build(), is("/hello-post"));
    }

    @Test
    public void test_pathFinder_findsFirstForMultipleUsages() {
        app.get("/hello-1", helloHandler);
        app.get("/hello-2", helloHandler);
        app.get("/hello-3", helloHandler);
        assertThat(app.pathBuilder(helloHandler).build(), is("/hello-1"));
    }

}

class SomeController {
    public static void methodRef(Context context) {
    }
    public static void methodRef2(Context context) {
    }
}

class ImplementingClass implements Handler {
    @Override
    public void handle(Context ctx) {
    }
}
