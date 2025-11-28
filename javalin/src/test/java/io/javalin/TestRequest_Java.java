package io.javalin;

import io.javalin.testing.TestUtil;
import org.junit.jupiter.api.Test;

import static io.javalin.testing.JavalinTestUtil.get;
import static org.assertj.core.api.Assertions.assertThat;

public class TestRequest_Java {

    @Test
    public void session_attribute_can_be_consumed_easily() {
        TestUtil.test((app, http) -> {
            get(app, "/store-attr", ctx -> ctx.sessionAttribute("attr", "Rowin"));
            get(app, "/read-attr", ctx -> ctx.result("" + ctx.consumeSessionAttribute("attr")));
            http.getBody("/store-attr");
            assertThat(http.getBody("/read-attr")).isEqualTo("Rowin"); // read (and consume) the attribute
            assertThat(http.getBody("/read-attr")).isEqualTo("null"); // fallback
        });
    }

}
