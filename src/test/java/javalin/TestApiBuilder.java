package javalin;

import org.junit.Test;

import static javalin.ApiBuilder.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestApiBuilder extends _UnirestBaseTest {

    @Test
    public void test_pathWorks_forGet() throws Exception {
        app.routes(() -> {
            get("/hello", simpleAnswer("Hello from level 0"));
            path("/level-1", () -> {
                get("/hello", simpleAnswer("Hello from level 1"));
                get("/hello-2", simpleAnswer("Hello again from level 1"));
                post("/create-1", simpleAnswer("Created something at level 1"));
                path("/level-2", () -> {
                    get("/hello", simpleAnswer("Hello from level 2"));
                    path("/level-3", () -> {
                        get("/hello", simpleAnswer("Hello from level 3"));
                    });
                });
            });
        });
        assertThat(GET_body("/hello"), is("Hello from level 0"));
        assertThat(GET_body("/level-1/hello"), is("Hello from level 1"));
        assertThat(GET_body("/level-1/level-2/hello"), is("Hello from level 2"));
        assertThat(GET_body("/level-1/level-2/level-3/hello"), is("Hello from level 3"));
    }

    private Handler simpleAnswer(String body) {
        return (req, res) -> res.body(body);
    }

    @Test
    public void test_pathWorks_forFilters() throws Exception {
        app.routes(() -> {
            path("/level-1", () -> {
                before("/*", (req, res) -> res.body("1"));
                path("/level-2", () -> {
                    path("/level-3", () -> {
                        get("/hello", updateAnswer("Hello"));
                    });
                    after("/*", updateAnswer("2"));
                });
            });
        });
        assertThat(GET_body("/level-1/level-2/level-3/hello"), is("1Hello2"));
    }

    private Handler updateAnswer(String body) {
        return (req, res) -> res.body(res.body() + body);
    }

}

