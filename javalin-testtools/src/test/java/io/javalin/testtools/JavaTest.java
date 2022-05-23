package io.javalin.testtools;

import io.javalin.Javalin;
import io.javalin.core.util.Header;
import kotlin.Pair;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JavaTest {

    static class MyJavaClass {
        public String field1;
        public String field2;

        public MyJavaClass() {
        }

        public MyJavaClass(String field1, String field2) {
            this.field1 = field1;
            this.field2 = field2;
        }
    }

    @Test
    public void get_method_works() {
        JavalinTest.test((server, client) -> {
            server.get("/hello", ctx -> ctx.result("Hello, World!"));
            Response response = client.get("/hello");
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).isEqualTo("Hello, World!");
        });
    }

    @Test
    public void can_do_query_params_and_headers() {
        JavalinTest.test((server, client) -> {
            server.get("/hello", ctx -> {
                String response = ctx.queryParam("from") + " " + ctx.header(Header.FROM);
                ctx.result(response);
            });
            Response response = client.get("/hello?from=From", req -> req.header(Header.FROM, "Russia With Love"));
            assertThat(response.body().string()).isEqualTo("From Russia With Love");
        });
    }

    @Test
    public void post_with_json_serialization_works() {
        JavalinTest.test((server, client) -> {
            server.post("/hello", ctx -> ctx.result(ctx.bodyAsClass(MyJavaClass.class).field1));
            Response response = client.post("/hello", new MyJavaClass("v1", "v2"));
            assertThat(response.body().string()).isEqualTo("v1");
        });
    }

    @Test
    public void all_common_verbs_work() {
        JavalinTest.test((server, client) -> {
            server.get("/", ctx -> ctx.result("GET"));
            assertThat(client.get("/").body().string()).isEqualTo("GET");

            server.post("/", ctx -> ctx.result("POST"));
            assertThat(client.post("/").body().string()).isEqualTo("POST");

            server.patch("/", ctx -> ctx.result("PATCH"));
            assertThat(client.patch("/").body().string()).isEqualTo("PATCH");

            server.put("/", ctx -> ctx.result("PUT"));
            assertThat(client.put("/").body().string()).isEqualTo("PUT");

            server.delete("/", ctx -> ctx.result("DELETE"));
            assertThat(client.delete("/").body().string()).isEqualTo("DELETE");
        });
    }

    @Test
    public void request_method_works() {
        JavalinTest.test((server, client) -> {
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
        JavalinTest.test(app, (server, client) -> {
            assertThat(client.get("/hello").body().string()).isEqualTo("Hello, World!");
        });
    }

    @Test
    public void capture_std_out_works() {
        Logger logger = LoggerFactory.getLogger(JavaTest.class);
        JavalinTest.test((server, client) -> {
            server.get("/hello", ctx -> {
                System.out.println("sout was called");
                logger.info("logger was called");
            });
            String stdOut = JavalinTest.captureStdOut(() -> client.get("/hello"));
            assertThat(stdOut).contains("sout was called");
            assertThat(stdOut).contains("logger was called");
        });
    }

    @Test
    public void testing_sse() {
        JavalinTest.test((server, client) -> {
            server.sse("/listen", (sseClient) -> {
                try {
                    for (int i = 0; i < 5; i++) {
                        sseClient.sendEvent("Hello!");
                        Thread.sleep(200);
                    }
                    sseClient.close();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            List<String> listOfEvents = new ArrayList<>();
            client.sse("/listen", new DefaultSseTestHandler() {
                @Override
                public void onMessage(SseEvent sseEvent) {
                    listOfEvents.add(sseEvent.getData());
                    if (listOfEvents.size() == 5) {
                        sseEvent.closeClient();
                        assert (true);
                    }
                }
            });
        });
    }

    @Test
    public void sse_to_invalid_url_should_trigger_failure() {
        JavalinTest.test((server, client) -> {
            client.sse("/url_that_does_not_exist", new DefaultSseTestHandler() {
                @Override
                public void onFailure(@NotNull SseFailure sseFailure) {
                    sseFailure.closeClient();
                    assert (true);
                }
            });
        });
    }

    @Test
    public void testing_full_app_works() {
        JavalinTest.test(JavaApp.app, (server, client) -> {
            assertThat(client.get("/hello").body().string()).isEqualTo("Hello, app!");
            assertThat(client.get("/hello/").body().string()).isEqualTo("Not found"); // JavaApp.app won't ignore trailing slashes
        });
    }

}
