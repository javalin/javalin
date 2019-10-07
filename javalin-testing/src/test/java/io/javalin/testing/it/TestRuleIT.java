package io.javalin.testing.it;

import io.javalin.testing.JavalinRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class TestRuleIT {

    @Rule
    public JavalinRule javalinRule1 = new JavalinRule((it) -> it.showJavalinBanner = false);

    @Test
    public void checkJavalinRuleWorks() {
        Assert.assertEquals(404, javalinRule1.getHttp().get("/not-found").getStatus());
    }
}
