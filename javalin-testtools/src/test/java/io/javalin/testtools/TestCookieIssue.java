package io.javalin.testtools;

import io.javalin.Javalin;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestCookieIssue {

    @Test
    public void canSetAndReadSessionAttributes() {
        JavalinTest.test(Javalin.create(config -> {
            config.routes.get("/set", ctx -> ctx.sessionAttribute("foo", "bar"));
            config.routes.get("/get", ctx -> ctx.result((String) ctx.sessionAttribute("foo")));
        }), (server, client) -> {
            client.get("/set"); // Set session attribute
            assertThat(client.get("/get").body().string()).isEqualTo("bar");
        });
    }

    @Test
    public void cookieHandlingWorksAutomatically() {
        JavalinTest.test(Javalin.create(config -> {
            config.routes.get("/set-cookie", ctx -> ctx.cookie("test-cookie", "cookie-value"));
            config.routes.get("/get-cookie", ctx -> ctx.result(ctx.cookie("test-cookie") != null ? ctx.cookie("test-cookie") : "no-cookie"));
        }), (server, client) -> {
            // Verify cookie is set in response
            var setCookieHeaders = client.get("/set-cookie").headers().get("Set-Cookie");
            assertThat(setCookieHeaders).isNotNull();
            assertThat(setCookieHeaders.get(0)).contains("test-cookie=cookie-value");

            // Verify cookie is automatically sent in subsequent request
            assertThat(client.get("/get-cookie").body().string()).isEqualTo("cookie-value");
        });
    }
}
