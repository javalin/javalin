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
            var res1 = client.get("/set");
            String res1Body = res1.body().string();
            var res2 = client.get("/get");
            String res2Body = res2.body().string();
            System.out.println("Response 1 Set-Cookie: " + res1.headers().get("Set-Cookie"));
            System.out.println("Response 2 body: " + res2Body);
            assertThat(res2Body).isEqualTo("bar");
        });
    }

    @Test
    public void cookieHandlingWorksAutomatically() {
        JavalinTest.test((server, client) -> {
            server.get("/set-cookie", ctx -> ctx.cookie("test-cookie", "cookie-value"));
            server.get("/get-cookie", ctx -> ctx.result(ctx.cookie("test-cookie") != null ? ctx.cookie("test-cookie") : "no-cookie"));
            
            var res1 = client.get("/set-cookie");
            String setCookieHeader = res1.headers().get("Set-Cookie");
            assertThat(setCookieHeader).contains("test-cookie=cookie-value");
            
            var res2 = client.get("/get-cookie");
            String res2Body = res2.body().string();
            assertThat(res2Body).isEqualTo("cookie-value");
        });
    }
}