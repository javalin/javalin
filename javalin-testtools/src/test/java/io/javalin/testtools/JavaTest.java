package io.javalin.testtools;

import io.javalin.Javalin;
import io.javalin.http.Header;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;

import static io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static io.javalin.http.HttpStatus.OK;
import static io.javalin.testtools.TestTool.TestLogsKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

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
        JavalinTest.test(Javalin.create(config -> {
            config.routes.get("/hello", ctx -> ctx.result("Hello, World!"));
        }), (server, client) -> {
            Response response = client.get("/hello");
            assertThat(response.code()).isEqualTo(OK.getCode());
            assertThat(response.body().string()).isEqualTo("Hello, World!");
        });
    }

    @Test
    public void can_do_query_params_and_headers() {
        JavalinTest.test(Javalin.create(config -> {
            config.routes.get("/hello", ctx -> {
                String response = ctx.queryParam("from") + " " + ctx.header(Header.FROM);
                ctx.result(response);
            });
        }), (server, client) -> {
            Response response = client.get("/hello?from=From", req -> req.header(Header.FROM, "Paris to Berlin"));
            assertThat(response.body().string()).isEqualTo("From Paris to Berlin");
        });
    }

    @Test
    public void post_with_json_serialization_works() {
        JavalinTest.test(Javalin.create(config -> {
            config.routes.post("/hello", ctx -> ctx.result(ctx.bodyAsClass(MyJavaClass.class).field1));
        }), (server, client) -> {
            Response response = client.post("/hello", new MyJavaClass("v1", "v2"));
            assertThat(response.body().string()).isEqualTo("v1");
        });
    }

    @Test
    public void all_common_verbs_work() {
        JavalinTest.test(Javalin.create(config -> {
            config.routes.get("/", ctx -> ctx.result("GET"));
            config.routes.post("/", ctx -> ctx.result("POST"));
            config.routes.patch("/", ctx -> ctx.result("PATCH"));
            config.routes.put("/", ctx -> ctx.result("PUT"));
            config.routes.delete("/", ctx -> ctx.result("DELETE"));
        }), (server, client) -> {
            assertThat(client.get("/").body().string()).isEqualTo("GET");
            assertThat(client.post("/").body().string()).isEqualTo("POST");
            assertThat(client.patch("/").body().string()).isEqualTo("PATCH");
            assertThat(client.put("/").body().string()).isEqualTo("PUT");
            assertThat(client.delete("/").body().string()).isEqualTo("DELETE");
        });
    }

    @Test
    public void request_method_works() {
        JavalinTest.test(Javalin.create(config -> {
            config.routes.post("/form", ctx -> ctx.result(ctx.formParam("username")));
        }), (server, client) -> {
            Response response = client.request("/form", requestBuilder -> {
                requestBuilder.post(new FormBody.Builder().add("username", "test").build());
            });
            assertThat(response.body().string()).isEqualTo("test");
        });
    }

    @Test
    public void custom_javalin_works() {
        Javalin app = Javalin.create(config -> {
            config.routes.get("/hello", ctx -> ctx.result("Hello, World!"));
        });
        JavalinTest.test(app, (server, client) -> {
            assertThat(client.get("/hello").body().string()).isEqualTo("Hello, World!");
        });
    }

    @Test
    public void capture_std_out_works() {
        Logger logger = LoggerFactory.getLogger(JavaTest.class);
        JavalinTest.test(Javalin.create(config -> {
            config.routes.get("/hello", ctx -> {
                System.out.println("sout was called");
                logger.info("logger was called");
            });
        }), (server, client) -> {
            String stdOut = JavalinTest.captureStdOut(() -> client.get("/hello"));
            assertThat(stdOut).contains("sout was called");
            assertThat(stdOut).contains("logger was called");
        });
    }

    @Test
    public void testing_full_app_works() {
        JavalinTest.test(new JavaApp().app, (server, client) -> {
            assertThat(client.get("/hello").body().string()).isEqualTo("Hello, app!");
            assertThat(client.get("/hello/").body().string()).isEqualTo("Endpoint GET /hello/ not found"); // JavaApp.app won't ignore trailing slashes
        });
    }

    @Test
    void custom_httpClient_is_used() {
        Javalin app = Javalin.create(config -> {
            config.routes.get("/hello", ctx -> ctx.result("Hello, " + ctx.header("X-Welcome") + "!"));
        });

        HttpClient customHttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        Map<String, String> defaultHeaders = Map.of("X-Welcome", "Javalin");
        TestConfig testConfig = new TestConfig(true, true, customHttpClient, defaultHeaders);

        JavalinTest.test(app, testConfig, (server, client) -> {
            assertThat(client.get("/hello").body().string()).isEqualTo("Hello, Javalin!");
        });
    }

    TestTool javalinTest = new TestTool(new TestConfig(false));

    @Test
    void instantiate_JavalinTestTool() {
        javalinTest.test(Javalin.create(config -> {
            config.routes.get("/hello", ctx -> ctx.result("Hello world"));
        }), (server, client) -> {
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
            JavalinTest.test(Javalin.create(config -> {
                config.routes.get("/hello", ctx -> {
                    throw new Exception("Error in handler code");
                });
            }), (server, client) -> {
                assertThat(client.get("/hello").code()).isEqualTo(INTERNAL_SERVER_ERROR.getCode());
            })
        );
    }

    @Test
    public void exception_in_handler_code_is_included_in_test_logs() {
        Javalin app = Javalin.create(config -> {
            config.routes.get("/hello", ctx -> {
                throw new Exception("Error in handler code");
            });
        });
        try {
            JavalinTest.test(app, (server, client) -> {
                assertThat(client.get("/hello").code()).isEqualTo(OK.getCode());
            });
        } catch (Throwable t) {
            // Ignore
        }
        assertThat(app.unsafe.appDataManager.get(TestLogsKey)).contains("Error in handler code");
    }

    @Test
    public void response_headers_are_accessible() {
        JavalinTest.test(Javalin.create(config -> {
            config.routes.get("/headers", ctx -> {
                ctx.header("Custom-Header", "custom-value");
                ctx.header("Another-Header", "another-value");
                ctx.result("Response with headers");
            });
        }), (server, client) -> {
            Response response = client.get("/headers");
            assertThat(response.headers().get("Custom-Header")).isNotNull().containsExactly("custom-value");
            assertThat(response.headers().get("Another-Header")).isNotNull().containsExactly("another-value");
            assertThat(response.headers().get("Non-Existent")).isNull();
        });
    }

    @Test
    public void empty_and_null_response_bodies_work() {
        JavalinTest.test(Javalin.create(config -> {
            config.routes.get("/empty", ctx -> ctx.result(""));
            config.routes.get("/null", ctx -> {}); // No result set
        }), (server, client) -> {
            assertThat(client.get("/empty").body().string()).isEqualTo("");
            assertThat(client.get("/null").body().string()).isEqualTo("");
        });
    }

    @Test
    public void request_builder_with_multiple_headers_works() {
        JavalinTest.test(Javalin.create(config -> {
            config.routes.post("/multi-headers", ctx -> ctx.result(
                "Auth: " + ctx.header("Authorization") + ", Accept: " + ctx.header("Accept") + ", Custom: " + ctx.header("X-Custom")));
        }), (server, client) -> {
            Response response = client.request("/multi-headers", builder ->
                builder.post(HttpRequest.BodyPublishers.ofString("test-body"))
                       .header("Authorization", "Bearer token123")
                       .header("Accept", "application/json")
                       .header("X-Custom", "test-value"));

            assertThat(response.body().string()).isEqualTo("Auth: Bearer token123, Accept: application/json, Custom: test-value");
        });
    }

    @Test
    public void different_http_methods_with_custom_bodies_work() {
        JavalinTest.test(Javalin.create(config -> {
            config.routes.put("/text", ctx -> ctx.result("PUT: " + ctx.body()));
            config.routes.patch("/text", ctx -> ctx.result("PATCH: " + ctx.body()));
            config.routes.delete("/text", ctx -> ctx.result("DELETE: " + ctx.body()));
        }), (server, client) -> {
            assertThat(client.request("/text", builder -> builder.put(HttpRequest.BodyPublishers.ofString("plain text")).header("Content-Type", "text/plain")).body().string()).isEqualTo("PUT: plain text");
            assertThat(client.request("/text", builder -> builder.patch(HttpRequest.BodyPublishers.ofString("patch data")).header("Content-Type", "text/plain")).body().string()).isEqualTo("PATCH: patch data");
            assertThat(client.request("/text", builder -> builder.delete(HttpRequest.BodyPublishers.ofString("delete data")).header("Content-Type", "text/plain")).body().string()).isEqualTo("DELETE: delete data");
        });
    }
}
