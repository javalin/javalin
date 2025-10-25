package io.javalin.javalinvue;

import io.javalin.http.Context;
import io.javalin.vue.VueComponent;
import io.javalin.vue.VueHandler;
import io.javalin.vue.VueRenderer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static io.javalin.testing.HttpClientExtensions.getBody;
import static io.javalin.testing.JavalinTestUtil.get;
import static org.assertj.core.api.Assertions.assertThat;

public class TestJavalinVueHandler {

    @Test
    public void testDefaultPreAndPostRenderer() {
        VueJavalinTest.test(null, null, (server, httpClient) -> {
            get(server, "/no-state", new VueHandler("test-component") {
            });
            String body = getBody(httpClient, "/no-state");
            assertThat(body).contains("<body><test-component></test-component></body>");
            assertThat(body).doesNotContain("PRE_RENDER");
            assertThat(body).doesNotContain("POST_RENDER");
        });
    }

    @Test
    public void testPreRenderer() {
        VueJavalinTest.test(null, null, (server, httpClient) -> {
            get(server, "/no-state", new VueHandler("test-component") {
                @NotNull
                @Override
                public String preRender(@NotNull String layout, @NotNull Context ctx) {
                    return layout.concat("PRE_RENDER");
                }
            });
            String body = getBody(httpClient, "/no-state");
            assertThat(body).contains("<body><test-component></test-component></body>");
            assertThat(body).contains("PRE_RENDER");
            assertThat(body).doesNotContain("POST_RENDER");
        });
    }

    @Test
    public void testPostRenderer() {
        VueJavalinTest.test(null, null, (server, httpClient) -> {
            get(server, "/no-state", new VueHandler("test-component") {
                @NotNull
                @Override
                public String postRender(@NotNull String layout, @NotNull Context ctx) {
                    return layout.concat("POST_RENDER");
                }
            });
            String body = getBody(httpClient, "/no-state");
            assertThat(body).contains("<body><test-component></test-component></body>");
            assertThat(body).doesNotContain("PRE_RENDER");
            assertThat(body).contains("POST_RENDER");
        });
    }

    @Test
    public void testVueRenderer() {
        VueJavalinTest.test(null, null, (server, httpClient) -> {
            get(server, "/no-state", new VueComponent("test-component", null, new VueRenderer() {
                @NotNull
                @Override
                public String postRender(@NotNull String layout, @NotNull Context ctx) {
                    return layout.concat("POST_RENDER");
                }

                @Override
                public String preRender(@NotNull String layout, @NotNull Context ctx) {
                    return layout.concat("PRE_RENDER");
                }
            }));
            String body = getBody(httpClient, "/no-state");
            assertThat(body).contains("<body><test-component></test-component></body>");
            assertThat(body).contains("POST_RENDER");
            assertThat(body).contains("PRE_RENDER");
        });
    }
}
