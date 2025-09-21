package io.javalin.testtools;

import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestCookieIssue {
    
    @Test
    public void reproduceIssue() {
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
    public void workaroundWorks() {
        JavalinTest.test((server, client) -> {
            server.get("/set", ctx -> ctx.sessionAttribute("foo", "bar"));
            server.get("/get", ctx -> ctx.result((String) ctx.sessionAttribute("foo")));
            var res1 = client.get("/set");
            String res1Body = res1.body().string();
            var res2 = client.get("/get", req -> req.header("Cookie", res1.headers().get("Set-Cookie")));
            String res2Body = res2.body().string();
            System.out.println("Response 1 Set-Cookie: " + res1.headers().get("Set-Cookie"));
            System.out.println("Response 2 body (workaround): " + res2Body);
            assertThat(res2Body).isEqualTo("bar");
        });
    }
}