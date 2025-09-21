package io.javalin.testtools;

import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestCookieIssue {
    
    @Test
    public void canSetAndReadSessionAttributes() {
        JavalinTest.test((server, client) -> {
            server.get("/set", ctx -> ctx.sessionAttribute("foo", "bar"));
            server.get("/get", ctx -> ctx.result((String) ctx.sessionAttribute("foo")));
            client.get("/set"); // Set session attribute
            assertThat(client.get("/get").body().string()).isEqualTo("bar");
        });
    }

    @Test
    public void cookieHandlingWorksAutomatically() {
        JavalinTest.test((server, client) -> {
            server.get("/set-cookie", ctx -> ctx.cookie("test-cookie", "cookie-value"));
            server.get("/get-cookie", ctx -> ctx.result(ctx.cookie("test-cookie") != null ? ctx.cookie("test-cookie") : "no-cookie"));
            
            // Verify cookie is set in response
            var setCookieHeaders = client.get("/set-cookie").headers().get("Set-Cookie");
            assertThat(setCookieHeaders).isNotNull();
            assertThat(setCookieHeaders.get(0)).contains("test-cookie=cookie-value");
            
            // Verify cookie is automatically sent in subsequent request
            assertThat(client.get("/get-cookie").body().string()).isEqualTo("cookie-value");
        });
    }
}