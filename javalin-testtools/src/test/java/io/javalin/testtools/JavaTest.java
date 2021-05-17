package io.javalin.testtools;

import io.javalin.Javalin;
import okhttp3.FormBody;
import okhttp3.Response;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JavaTest {

    @Test
    public void normal_get_method_works() {
        TestUtil.test((server, client) -> {
            server.get("/hello", ctx -> ctx.result("Hello, World!"));
            Response response = client.get("/hello");
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).isEqualTo("Hello, World!");
        });
    }

    @Test
    public void getBody_method_works() {
        TestUtil.test((server, client) -> {
            server.get("/hello", ctx -> ctx.result("Hello, World!"));
            assertThat(client.getBody("/hello")).isEqualTo("Hello, World!");
        });
    }

    @Test
    public void request_method_works() {
        TestUtil.test((server, client) -> {
            server.post("/form", ctx -> ctx.result(ctx.formParam("username")));
            Response response = client.request("/form", requestBuilder -> {
                requestBuilder.post(new FormBody.Builder().add("username", "test").build());
            });
            assertThat(response.body().string()).isEqualTo("test");
        });
    }

    @Test
    public void custom_javalin_works() {
        Javalin app = Javalin.create()
            .get("/hello", ctx -> ctx.result("Hello, World!"));
        TestUtil.test(app, (server, client) -> {
            assertThat(client.getBody("/hello")).isEqualTo("Hello, World!");
        });
    }

    @Test
    public void capture_std_out_works() {
        Logger logger = LoggerFactory.getLogger(JavaTest.class);
        TestUtil.test((server, client) -> {
            server.get("/hello", ctx -> {
                System.out.println("sout was called");
                logger.info("logger was called");
            });
            String stdOut = TestUtil.captureStdOut(() -> client.getBody("/hello"));
            assertThat(stdOut).contains("sout was called");
            assertThat(stdOut).contains("logger was called");
        });
    }

}
