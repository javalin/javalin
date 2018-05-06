package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestExtension {

    private static Javalin app;
    private static String origin = null;

    @BeforeClass
    public static void setup() throws IOException {
        app = Javalin.create()
                .port(0)
                .start();
        origin = "http://localhost:" + app.port();
    }

    @After
    public void clear() {
        app.clearMatcherAndMappers();
    }

    @AfterClass
    public static void tearDown() {
        app.stop();
    }

    @Test
    public void test_helloWorldExtension() throws Exception {
        app.register((app) -> {
            app.before("/protected", ctx -> {
                throw new HaltException(401, "Protected");
            });
            app.get("/", ctx -> ctx.result("Hello world"));
        });

        HttpResponse<String> response = Unirest.get(origin + "/").asString();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), is("Hello world"));

        HttpResponse<String> response2 = Unirest.get(origin + "/protected").asString();
        assertThat(response2.getStatus(), is(401));
        assertThat(response2.getBody(), is("Protected"));
    }

    @Test
    public void test_javaClassExtension() throws Exception {
        app.register(new JavaClassExtension("Foobar!"));

        HttpResponse<String> response = Unirest.get(origin + "/").asString();
        assertThat(response.getStatus(), is(400));
        assertThat(response.getBody(), is("Foobar!"));
    }

    static class JavaClassExtension implements Extension {
        private final String magicValue;

        public JavaClassExtension(String magicValue) {
            this.magicValue = magicValue;
        }

        @Override
        public void register(Javalin app) {
            app.before(ctx -> {
                throw new HaltException(400, magicValue);
            });
        }
    }

}
