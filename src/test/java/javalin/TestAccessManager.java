package javalin;

import org.junit.Test;

import javalin.security.AccessManager;
import javalin.security.Role;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import static javalin.ApiBuilder.*;
import static javalin.TestAccessManager.MyRoles.*;
import static javalin.security.Role.roles;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestAccessManager {

    private AccessManager accessManager = (handler, request, response, permittedRoles) -> {
        String userRole = request.queryParam("role");
        if (userRole != null && permittedRoles.contains(MyRoles.valueOf(userRole))) {
            handler.handle(request, response);
        } else {
            response.status(401).body("Unauthorized");
        }
    };

    enum MyRoles implements Role {
        ROLE_ONE, ROLE_TWO, ROLE_THREE;
    }

    static String origin = "http://localhost:1234";

    @Test
    public void test_noAccessManager_throwsException() throws Exception {
        Javalin app = Javalin.create().port(1234).start().awaitInitialization();
        app.get("/secured", (req, res) -> res.body("Hello"), roles(ROLE_ONE));
        assertThat(callWithRole("/secured", "ROLE_ONE"), is("Internal server error"));
        app.stop().awaitTermination();
    }

    @Test
    public void test_accessManager_restrictsAccess() throws Exception {
        Javalin app = Javalin.create().port(1234).start().awaitInitialization();
        app.accessManager(accessManager);
        app.get("/secured", (req, res) -> res.body("Hello"), roles(ROLE_ONE, ROLE_TWO));
        assertThat(callWithRole("/secured", "ROLE_ONE"), is("Hello"));
        assertThat(callWithRole("/secured", "ROLE_TWO"), is("Hello"));
        assertThat(callWithRole("/secured", "ROLE_THREE"), is("Unauthorized"));
        app.stop().awaitTermination();
    }

    @Test
    public void test_accessManager_restrictsAccess_forStaticApi() throws Exception {
        Javalin app = Javalin.create().port(1234).start().awaitInitialization();
        app.accessManager(accessManager);
        app.routes(() -> {
            get("/static-secured", (req, res) -> res.body("Hello"), roles(ROLE_ONE, ROLE_TWO));
        });
        assertThat(callWithRole("/static-secured", "ROLE_ONE"), is("Hello"));
        assertThat(callWithRole("/static-secured", "ROLE_TWO"), is("Hello"));
        assertThat(callWithRole("/static-secured", "ROLE_THREE"), is("Unauthorized"));
        app.stop().awaitTermination();
    }

    private String callWithRole(String path, String role) throws UnirestException {
        return Unirest.get(origin + path).queryString("role", role).asString().getBody();
    }

}
