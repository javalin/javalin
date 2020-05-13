package io.javalin.testing.it;

import io.javalin.Javalin;
import io.javalin.testing.TestUtil;
import org.junit.Assert;
import org.junit.Test;

public class TestUtilIT {
    @Test
    public void checkTestUtilWorks() {
        TestUtil.test(
            Javalin.create(),
            (app, http) -> Assert.assertEquals(404, http.get("/not-found").getStatus()));
    }
}
