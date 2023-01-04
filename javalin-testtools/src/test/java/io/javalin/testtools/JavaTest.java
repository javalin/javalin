package io.javalin.testtools;

import io.javalin.Javalin;
import io.javalin.http.Header;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.javalin.http.HttpStatus.OK;
import static io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static io.javalin.http.HttpStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
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
            assertThat(response.code()).isEqualTo(OK.getCode());
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
            Response response = client.get("/hello?from=From", req -> req.header(Header.FROM, "Paris to Berlin"));
            assertThat(response.body().string()).isEqualTo("From Paris to Berlin");
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
    public void testing_full_app_works() {
        JavalinTest.test(new JavaApp().app, (server, client) -> {
            assertThat(client.get("/hello").body().string()).isEqualTo("Hello, app!");
            assertThat(client.get("/hello/").body().string()).isEqualTo(NOT_FOUND.getMessage()); // JavaApp.app won't ignore trailing slashes
        });
    }

    @Test
    void custom_okHttpClient_is_used() {
        Javalin app = Javalin.create()
            .get("/hello", ctx -> ctx.result("Hello, " + ctx.header("X-Welcome") + "!"));

        OkHttpClient okHttpClientAddingHeader = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                Request userRequest = chain.request();
                return chain.proceed(userRequest.newBuilder()
                    .addHeader("X-Welcome", "Javalin")
                    .build());
            })
            .build();

        TestConfig config = new TestConfig(true, true, okHttpClientAddingHeader);

        JavalinTest.test(app, config, (server, client) -> {
            assertThat(client.get("/hello").body().string()).isEqualTo("Hello, Javalin!");
        });
    }

    TestTool javalinTest = new TestTool(new TestConfig(false));

    @Test
    void instantiate_JavalinTestTool() {
        javalinTest.test((server, client) -> {
            server.get("/hello", ctx -> ctx.result("Hello world"));
            assertThat(client.get("/hello").body().string()).isEqualTo("Hello world");
        });
    }

    public void exceptions_in_test_code_get_rethrown() {
        assertThatExceptionOfType(Exception.class).isThrownBy(() ->
            JavalinTest.test((server, client) -> {
                throw new Exception("Error in test code");
            })
        ).withMessageMatching("Error in test code");
    }

    @Test
    public void exceptions_in_handler_code_are_caught_by_exception_handler_and_not_thrown() {
        assertThatNoException().isThrownBy(() ->
            JavalinTest.test((server, client) -> {
                server.get("/hello", ctx -> {
                    throw new Exception("Error in handler code");
                });

                assertThat(client.get("/hello").code()).isEqualTo(INTERNAL_SERVER_ERROR.getCode());
            })
        );
    }

    @Test
    public void exception_in_handler_code_is_included_in_test_logs() {
        Javalin app = Javalin.create();

        try {
            JavalinTest.test(app, (server, client) -> {
                server.get("/hello", ctx -> {
                    throw new Exception("Error in handler code");
                });

                assertThat(client.get("/hello").code()).isEqualTo(OK.getCode());
            });
        } catch (Throwable t) {
            // Ignore
        }

        assertThat((String) app.attribute("testlogs")).contains("Error in handler code");
    }
}
