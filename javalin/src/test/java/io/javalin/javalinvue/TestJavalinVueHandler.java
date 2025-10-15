package io.javalin.javalinvue;

import io.javalin.http.Context;
import io.javalin.vue.VueComponent;
import io.javalin.vue.VueHandler;
import io.javalin.vue.VueRenderer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static io.javalin.testing.JavalinTestUtil.*;

public class TestJavalinVueHandler {

    @Test
    public void testDefaultPreAndPostRenderer() {
        VueTestUtil.test((server, httpUtil) -> {
            get(server, "/no-state", new VueHandler("test-component") {
            });
            String body = httpUtil.getBody("/no-state");
            assertThat(body).contains("<body><test-component></test-component></body>");
            assertThat(body).doesNotContain("PRE_RENDER");
            assertThat(body).doesNotContain("POST_RENDER");
        });
    }

    @Test
    public void testPreRenderer() {
        VueTestUtil.test((server, httpUtil) -> {
            get(server, "/no-state", new VueHandler("test-component") {
                @NotNull
                @Override
                public String preRender(@NotNull String layout, @NotNull Context ctx) {
                    return layout.concat("PRE_RENDER");
                }
            });
            String body = httpUtil.getBody("/no-state");
            assertThat(body).contains("<body><test-component></test-component></body>");
            assertThat(body).contains("PRE_RENDER");
            assertThat(body).doesNotContain("POST_RENDER");
        });
    }

    @Test
    public void testPostRenderer() {
        VueTestUtil.test((server, httpUtil) -> {
            get(server, "/no-state", new VueHandler("test-component") {
                @NotNull
                @Override
                public String postRender(@NotNull String layout, @NotNull Context ctx) {
                    return layout.concat("POST_RENDER");
                }
            });
            String body = httpUtil.getBody("/no-state");
            assertThat(body).contains("<body><test-component></test-component></body>");
            assertThat(body).doesNotContain("PRE_RENDER");
            assertThat(body).contains("POST_RENDER");
        });
    }

    @Test
    public void testVueRenderer() {
        VueTestUtil.test((server, httpUtil) -> {
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
            String body = httpUtil.getBody("/no-state");
            assertThat(body).contains("<body><test-component></test-component></body>");
            assertThat(body).contains("POST_RENDER");
            assertThat(body).contains("PRE_RENDER");
        });
    }
}
