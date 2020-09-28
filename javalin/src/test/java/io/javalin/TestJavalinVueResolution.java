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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author tareq
 */
public class TestJavalinVueResolution {

    static {

        JavalinVue.resolveDependencies = true;
    }

    @Test
    public void resolveSingleDependencyTest() {
        TestUtil.test((server, httpUtil) -> {
            server.get("/single-view", new VueComponent("<view-one></view-one>"));
            String body = httpUtil.get("/single-view").getBody();
            assertTrue(body.contains("<body><view-one></view-one></body>"));
            assertFalse(body.contains("<view-two>"));
            assertFalse(body.contains("<view-three>"));
            assertFalse(body.contains("<view-nested-dependency>"));
            assertTrue(body.contains("dependency-one"));
            assertFalse(body.contains("dependency-two"));
            assertFalse(body.contains("dependency-three"));
            assertFalse(body.contains("dependency-four"));
            assertFalse(body.contains("nested-dependency"));
        });
    }

    @Test
    public void resolveNestedDependencyTest() {
        TestUtil.test((server, httpUtil) -> {
            server.get("/nested-view", new VueComponent("<view-nested-dependency></view-nested-dependency>"));
            String body = httpUtil.get("/nested-view").getBody();
            assertFalse(body.contains("<view-one>"));
            assertFalse(body.contains("<view-two>"));
            assertFalse(body.contains("<view-three>"));
            assertTrue(body.contains("<body><view-nested-dependency></view-nested-dependency></body>"));
            assertTrue(body.contains("dependency-one"));
            assertTrue(body.contains("dependency-two"));
            assertFalse(body.contains("dependency-three"));
            assertFalse(body.contains("dependency-four"));
            assertTrue(body.contains("nested-dependency"));
        });
    }

    @Test
    public void resolveMultiComponentFileDependencyTest() {
        TestUtil.test((server, httpUtil) -> {
            server.get("/multi-view-one", new VueComponent("<view-two></view-two>"));
            String body = httpUtil.get("/multi-view-one").getBody();
            assertFalse(body.contains("<view-one>"));
            assertTrue(body.contains("<body><view-two></view-two></body>"));
            assertFalse(body.contains("<view-three>"));
            assertFalse(body.contains("<view-nested-dependency>"));
            assertFalse(body.contains("dependency-one"));
            assertFalse(body.contains("dependency-two"));
            assertTrue(body.contains("dependency-three"));
            assertTrue(body.contains("dependency-four"));
            assertFalse(body.contains("nested-dependency"));

            server.get("/multi-view-two", new VueComponent("<view-three></view-three>"));
            body = httpUtil.get("/multi-view-two").getBody();
            assertFalse(body.contains("<view-one>"));
            assertFalse(body.contains("<view-two>"));
            assertTrue(body.contains("<body><view-three></view-three></body>"));
            assertFalse(body.contains("<view-nested-dependency>"));
            assertFalse(body.contains("dependency-one"));
            assertFalse(body.contains("dependency-two"));
            assertTrue(body.contains("dependency-three"));
            assertTrue(body.contains("dependency-four"));
            assertFalse(body.contains("nested-dependency"));
        });
    }

}
