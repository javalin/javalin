package javalin;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestMultipleInstances {

    private static Javalin app1;
    private static Javalin app2;
    private static Javalin app3;

    @BeforeClass
    public static void setup() throws IOException {
        app1 = Javalin.create().port(7001).start().awaitInitialization();
        app2 = Javalin.create().port(7002).start().awaitInitialization();
        app3 = Javalin.create().port(7003).start().awaitInitialization();
    }

    @AfterClass
    public static void tearDown() {
        app1.stop();
        app2.stop();
        app3.stop();
    }

    @Test
    public void test_getMultiple() throws Exception {
        app1.get("/hello-1", (req, res) -> res.body("Hello first World"));
        app2.get("/hello-2", (req, res) -> res.body("Hello second World"));
        app3.get("/hello-3", (req, res) -> res.body("Hello third World"));
        assertThat(getBody("7001", "/hello-1"), is("Hello first World"));
        assertThat(getBody("7002", "/hello-2"), is("Hello second World"));
        assertThat(getBody("7003", "/hello-3"), is("Hello third World"));
    }

    static String getBody(String port, String pathname) throws UnirestException {
        return Unirest.get("http://localhost:" + port + pathname).asString().getBody();
    }

}
