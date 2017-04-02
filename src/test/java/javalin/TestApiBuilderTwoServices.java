package javalin;

import org.junit.Test;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import static javalin.ApiBuilder.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

public class TestApiBuilderTwoServices {

    @Test
    public void testApiBuilder_twoServices() throws Exception {
        Javalin app1 = Javalin.create().port(55555).start().awaitInitialization();
        Javalin app2 = Javalin.create().port(55556).start().awaitInitialization();
        app1.routes(() -> {
            get("/hello1", (req, res) -> res.body("Hello1"));
        });
        app2.routes(() -> {
            get("/hello1", (req, res) -> res.body("Hello1"));
        });
        app1.routes(() -> {
            get("/hello2", (req, res) -> res.body("Hello2"));
        });
        app2.routes(() -> {
            get("/hello2", (req, res) -> res.body("Hello2"));
        });
        assertThat(call(55555, "/hello1"), is("Hello1"));
        assertThat(call(55556, "/hello1"), is("Hello1"));
        assertThat(call(55555, "/hello2"), is("Hello2"));
        assertThat(call(55556, "/hello2"), is("Hello2"));
        app1.stop().awaitTermination();
        app2.stop().awaitTermination();
    }

    private String call(int port, String path) throws UnirestException {
        return Unirest.get("http://localhost:" + port + path).asString().getBody();
    }

}
