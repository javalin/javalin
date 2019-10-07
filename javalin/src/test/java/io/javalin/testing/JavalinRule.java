package io.javalin.testing;

import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import java.util.function.Consumer;
import org.junit.rules.ExternalResource;

public class JavalinRule extends ExternalResource {

    private final Consumer<JavalinConfig> config;

    private Javalin javalin;
    private HttpUtil http;

    public JavalinRule(Consumer<JavalinConfig> config) {
        this.config = config;
    }

    public Javalin getJavalin() {
        return javalin;
    }

    public HttpUtil getHttp() {
        return http;
    }

    @Override
    protected void before() {
        javalin = Javalin.create(config);
        http = TestUtil.startAndCreateHttpUtil(javalin);
    }

    @Override
    protected void after() {
        TestUtil.cleanup(javalin, http);
    }
}
