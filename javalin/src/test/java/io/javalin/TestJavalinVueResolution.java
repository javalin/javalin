/*
 * Copyright 2020 tareq.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.javalin;

import io.javalin.plugin.rendering.vue.JavalinVue;
import io.javalin.plugin.rendering.vue.VueComponent;
import io.javalin.testing.TestUtil;
import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author tareq
 */
public class TestJavalinVueResolution {

    @Before
    public void resetJavalinVue() {
        TestJavalinVue.Companion.before();
        JavalinVue.optimizeDependencies = true;
    }

    @Test
    public void resoleAllDependenciesTest() {
        TestUtil.test((server, httpUtil) -> {
            JavalinVue.optimizeDependencies = false;
            server.get("/non-optimized", new VueComponent("<test-component></test-component>"));
            String body = httpUtil.getBody("/non-optimized");
            assertThat(body).contains("<body><test-component></test-component></body>");
            assertThat(body).contains("view-one");
            assertThat(body).contains("view-two");
            assertThat(body).contains("view-three");
            assertThat(body).contains("dependency-one");
            assertThat(body).contains("dependency-two");
            assertThat(body).contains("dependency-three");
            assertThat(body).contains("dependency-four");
            assertThat(body).contains("nested-dependency");
            assertThat(body).contains("view-nested-dependency");
            assertThat(body).contains("multi-dependency");
        });
    }

    @Test
    public void resolveSingleDependencyTest() {
        TestUtil.test((server, httpUtil) -> {
            server.get("/single-view", new VueComponent("<view-one></view-one>"));
            String body = httpUtil.getBody("/single-view");
            assertThat(body).contains("<body><view-one></view-one></body>");
            assertThat(body).doesNotContain("<view-two>");
            assertThat(body).doesNotContain("<view-three>");
            assertThat(body).doesNotContain("<view-nested-dependency>");
            assertThat(body).contains("dependency-one");
            assertThat(body).doesNotContain("dependency-two");
            assertThat(body).doesNotContain("dependency-three");
            assertThat(body).doesNotContain("dependency-four");
            assertThat(body).doesNotContain("nested-dependency");
        });
    }

    @Test
    public void resolveVue3DependencyTest() {
        TestUtil.test((server, httpUtil) -> {
            JavalinVue.vueVersion(vueVersion -> vueVersion.vue3("app"));
            server.get("/single-view", new VueComponent("<view-one-3></view-one-3>"));
            String body = httpUtil.getBody("/single-view");
            assertThat(body).contains("<body><view-one-3></view-one-3></body>");
            assertThat(body).doesNotContain("<view-two-3>");
            assertThat(body).doesNotContain("<view-three-3>");
            assertThat(body).doesNotContain("<view-nested-dependency-3>");
            assertThat(body).doesNotContain("<view-two>");
            assertThat(body).doesNotContain("<view-three>");
            assertThat(body).doesNotContain("<view-nested-dependency>");
            assertThat(body).contains("dependency-one");
            assertThat(body).contains("dependency-one-3");
            assertThat(body).doesNotContain("dependency-two");
            assertThat(body).doesNotContain("dependency-three");
            assertThat(body).doesNotContain("dependency-four");
            assertThat(body).doesNotContain("nested-dependency");
            assertThat(body).doesNotContain("dependency-two-3");
            assertThat(body).doesNotContain("dependency-three");
            assertThat(body).doesNotContain("dependency-four");
            assertThat(body).doesNotContain("nested-dependency");
            assertThat(body).doesNotContain("Vue.component");
            assertThat(body).contains("app.component");
        });
    }

    @Test
    public void resolveNestedDependencyTest() {
        TestUtil.test((server, httpUtil) -> {
            server.get("/nested-view", new VueComponent("<view-nested-dependency></view-nested-dependency>"));
            String body = httpUtil.getBody("/nested-view");
            assertThat(body).doesNotContain("<view-one>");
            assertThat(body).doesNotContain("<view-two>");
            assertThat(body).doesNotContain("<view-three>");
            assertThat(body).contains("<body><view-nested-dependency></view-nested-dependency></body>");
            assertThat(body).contains("dependency-one");
            assertThat(body).contains("dependency-two");
            assertThat(body).doesNotContain("dependency-three");
            assertThat(body).doesNotContain("dependency-four");
            assertThat(body).contains("nested-dependency");
        });
    }

    @Test
    public void resolveMultiComponentFileDependencyTest() {
        TestUtil.test((server, httpUtil) -> {
            server.get("/multi-view-one", new VueComponent("<view-two></view-two>"));
            String body = httpUtil.getBody("/multi-view-one");
            assertThat(body).doesNotContain("<view-one>");
            assertThat(body).contains("<body><view-two></view-two></body>");
            assertThat(body).doesNotContain("<view-three>");
            assertThat(body).doesNotContain("<view-nested-dependency>");
            assertThat(body).doesNotContain("dependency-one");
            assertThat(body).doesNotContain("dependency-two");
            assertThat(body).contains("dependency-three");
            assertThat(body).contains("dependency-four");
            assertThat(body).doesNotContain("nested-dependency");

            server.get("/multi-view-two", new VueComponent("<view-three></view-three>"));
            body = httpUtil.getBody("/multi-view-two");
            assertThat(body).doesNotContain("<view-one>");
            assertThat(body).doesNotContain("<view-two>");
            assertThat(body).contains("<body><view-three></view-three></body>");
            assertThat(body).doesNotContain("<view-nested-dependency>");
            assertThat(body).doesNotContain("dependency-one");
            assertThat(body).doesNotContain("dependency-two");
            assertThat(body).contains("dependency-three");
            assertThat(body).contains("dependency-four");
            assertThat(body).doesNotContain("nested-dependency");
        });
    }

    @Test
    public void componentWithNumberTest() {
        TestUtil.test((server, httpUtil) -> {
            server.get("/multi-view-number", new VueComponent("<view-number-dependency></view-number-dependency>"));
            String body = httpUtil.getBody("/multi-view-number");
            assertThat(body).contains("<dependency-1></dependency-1>");
            assertThat(body).contains("<dependency-1-foo></dependency-1-foo>");
            assertThat(body).contains("Vue.component(\"view-number-dependency\",{template:\"#view-number-dependency\"})");
            assertThat(body).contains("Vue.component('dependency-1',{template:\"#dependency-1\"})");
            assertThat(body).contains("Vue.component('dependency-1-foo',{template:\"#dependency-1-foo\"})");
            assertThat(body).doesNotContain("Vue.component('dependency-123',{template:\"#dependency-123\"})");
            assertThat(body).doesNotContain("<dependency-123");
        });
    }

    @Test
    public void componentWithMultilineComponentsUsageTest() {
        TestUtil.test((server, httpUtil) -> {
            server.get("/multiline-view-number", new VueComponent("<view-multiline-dependency></view-multiline-dependency>"));
            String body = httpUtil.getBody("/multiline-view-number");
            assertThat(body).contains("Vue.component(\"view-multiline-dependency\",{template:\"#view-multiline-dependency\"})");
            assertThat(body).contains("Vue.component('dependency-1',{template:\"#dependency-1\"})");
            assertThat(body).contains("Vue.component('dependency-1-foo',{template:\"#dependency-1-foo\"})");
            assertThat(body).contains("Vue.component('dependency-one',{template:\"#dependency-one\"})");
            assertThat(body).doesNotContain("Vue.component('dependency-123',{template:\"#dependency-123\"})");
            assertThat(body).doesNotContain("<dependency-123");
        });
    }

}
