package io.javalin;

import io.javalin.plugin.rendering.vue.JavalinVue;
import io.javalin.plugin.rendering.vue.VueVersion;
import io.javalin.plugin.rendering.vue.VueVersionConfig;
import java.util.HashMap;
import org.junit.Test;

public class TestJavalinVue_Java {

    @Test
    public void api_looks_nice_from_java() {
        JavalinVue.optimizeDependencies = false;
        JavalinVue.cacheControl = "strings are great";
        JavalinVue.isDevFunction = ctx -> true;
        JavalinVue.stateFunction = ctx -> new HashMap<String, Object>();
        JavalinVue.vueDirectory(vueDir -> vueDir.classpathPath("/vue"));
        JavalinVue.vueDirectory(vueDir -> vueDir.classpathPath("/vue", TestJavalinVue_Java.class));
        JavalinVue.vueDirectory(vueDir -> vueDir.externalPath("/vue"));
        JavalinVue.vueVersion(VueVersionConfig::vue2);
        JavalinVue.vueVersion(vueVersion -> vueVersion.vue3("MyApp"));
    }

}
