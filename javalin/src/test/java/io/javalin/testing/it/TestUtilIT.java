package io.javalin.testing.it;

import io.javalin.Javalin;
import io.javalin.testing.TestUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUtilIT {
    @Test
    public void checkTestUtilWorks() {
        TestUtil.test(
            Javalin.create(),
            (app, http) -> assertEquals(404, http.get("/not-found").getStatus()));
    }
}
