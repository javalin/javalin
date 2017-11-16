/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.javalin.security.AccessManager;
import io.javalin.security.Role;
import org.junit.Test;
import static io.javalin.ApiBuilder.get;
import static io.javalin.TestAccessManager.MyRoles.ROLE_ONE;
import static io.javalin.TestAccessManager.MyRoles.ROLE_TWO;
import static io.javalin.security.Role.roles;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestAccessManager {

    private AccessManager accessManager = (handler, ctx, permittedRoles) -> {
        String userRole = ctx.queryParam("role");
        if (userRole != null && permittedRoles.contains(MyRoles.valueOf(userRole))) {
            handler.handle(ctx);
        } else {
            ctx.status(401).result("Unauthorized");
        }
    };

    enum MyRoles implements Role {
        ROLE_ONE, ROLE_TWO, ROLE_THREE;
    }

    static String origin = "http://localhost:1234";

    @Test
    public void test_noAccessManager_throwsException() throws Exception {
        Javalin app = Javalin.create().port(1234).start();
        app.get("/secured", ctx -> ctx.result("Hello"), roles(ROLE_ONE));
        assertThat(callWithRole("/secured", "ROLE_ONE"), is("Internal server error"));
        app.stop();
    }

    @Test
    public void test_accessManager_restrictsAccess() throws Exception {
        Javalin app = Javalin.create().port(1234).start();
        app.accessManager(accessManager);
        app.get("/secured", ctx -> ctx.result("Hello"), roles(ROLE_ONE, ROLE_TWO));
        assertThat(callWithRole("/secured", "ROLE_ONE"), is("Hello"));
        assertThat(callWithRole("/secured", "ROLE_TWO"), is("Hello"));
        assertThat(callWithRole("/secured", "ROLE_THREE"), is("Unauthorized"));
        app.stop();
    }

    @Test
    public void test_accessManager_restrictsAccess_forStaticApi() throws Exception {
        Javalin app = Javalin.create().port(1234).start();
        app.accessManager(accessManager);
        app.routes(() -> {
            get("/static-secured", ctx -> ctx.result("Hello"), roles(ROLE_ONE, ROLE_TWO));
        });
        assertThat(callWithRole("/static-secured", "ROLE_ONE"), is("Hello"));
        assertThat(callWithRole("/static-secured", "ROLE_TWO"), is("Hello"));
        assertThat(callWithRole("/static-secured", "ROLE_THREE"), is("Unauthorized"));
        app.stop();
    }

    private String callWithRole(String path, String role) throws UnirestException {
        return Unirest.get(origin + path).queryString("role", role).asString().getBody();
    }

}
