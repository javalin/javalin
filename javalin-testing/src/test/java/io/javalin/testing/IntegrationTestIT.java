package io.javalin.testing;

import org.junit.Assert;
import org.junit.Test;

public class IntegrationTestIT {
    @Test
    public void checkTestUtilWorks() {
        TestUtil.test(io.javalin.Javalin.create(), (app, http) -> {
            Assert.assertEquals(404, http.get("/not-found").getStatus());
        });
    }
}
