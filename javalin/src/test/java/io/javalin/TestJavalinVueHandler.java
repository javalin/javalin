package io.javalin;

import io.javalin.http.Context;
import io.javalin.plugin.rendering.vue.JavalinVue;
import io.javalin.plugin.rendering.vue.VueComponent;
import io.javalin.plugin.rendering.vue.VueHandler;
import io.javalin.plugin.rendering.vue.VueRenderer;
import io.javalin.testing.TestUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestJavalinVueHandler {

    @BeforeEach
    public void resetJavalinVue() {
        TestJavalinVue.Companion.before();
        JavalinVue.optimizeDependencies = true;
    }
    @Test
    public void testDefaultPreAndPostRenderer() {
        TestUtil.test((server, httpUtil) -> {
            server.get("/no-state", new VueHandler(){

                @NotNull
                @Override
                public String component(@NotNull Context ctx) {
                    return "test-component";
                }
            });
            String body = httpUtil.getBody("/no-state");
            assertThat(body).contains("<body><test-component></test-component></body>");
            assertThat(body).doesNotContain("PRE_RENDER");
            assertThat(body).doesNotContain("POST_RENDER");
        });
    }

    @Test
    public void testPreRenderer() {
        TestUtil.test((server, httpUtil) -> {
            server.get("/no-state", new VueHandler(){

                @NotNull
                @Override
                public String component(@NotNull Context ctx) {
                    return "test-component";
                }

                @NotNull
                @Override
                public String preRender(@NotNull String template, @NotNull Context ctx) {
                    return template.concat("PRE_RENDER");
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
        TestUtil.test((server, httpUtil) -> {
            server.get("/no-state", new VueHandler(){

                @NotNull
                @Override
                public String component(@NotNull Context ctx) {
                    return "test-component";
                }

                @NotNull
                @Override
                public String postRender(@NotNull String template, @NotNull Context ctx) {
                    return template.concat("POST_RENDER");
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
        TestUtil.test((server, httpUtil) -> {
            server.get("/no-state", new VueComponent("test-component",null, new VueRenderer(){
                @NotNull
                @Override
                public String postRender(@NotNull String template, @NotNull Context ctx) {
                    return template.concat("POST_RENDER");
                }

                @Override
                public String preRender(@NotNull String template, @NotNull Context ctx){
                    return template.concat("PRE_RENDER");
                }
            }));
            String body = httpUtil.getBody("/no-state");
            assertThat(body).contains("<body><test-component></test-component></body>");
            assertThat(body).contains("POST_RENDER");
            assertThat(body).contains("PRE_RENDER");
        });
    }
}
