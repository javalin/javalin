/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.testing.TestUtil;
import io.javalin.validation.JavalinValidation;
import io.javalin.validation.Validator;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class TestValidation_Java {
    private static class CustomException extends RuntimeException {
        public CustomException(String message) {
            super(message);
        }
    }

    @Test
    public void validation_on_context_works_from_java() {
        TestUtil.test((app, http) -> {
            app.get("/validate", ctx -> ctx.queryParamAsClass("param", Integer.class).get());
            assertThat(http.getBody("/validate?param=hmm")).contains("TYPE_CONVERSION_FAILED");
        });
    }

    @Test
    public void default_values_work_from_java() {
        TestUtil.test((app, http) -> {
            app.get("/validate", ctx -> ctx.result(ctx.queryParamAsClass("param", Integer.class).getOrDefault(250).toString()));
            assertThat(http.getBody("/validate?param=hmm")).contains("TYPE_CONVERSION_FAILED");
            assertThat(http.getBody("/validate")).isEqualTo("250");
        });
    }

    @Test
    public void get_or_throw_works_from_java() {
        TestUtil.test((app, http) -> {
            app.get("/", ctx -> {
                Integer myInt = ctx.queryParamAsClass("my-qp", Integer.class)
                    .getOrThrow(e -> new CustomException("'my-qp' is not a number"));
                ctx.result(myInt.toString());
            });
            app.exception(CustomException.class, (e, ctx) -> ctx.result(e.getMessage()));
            assertThat(http.getBody("/")).isEqualTo("'my-qp' is not a number");
        });
    }

    @Test
    public void queryParams_can_be_used_to_validate_list() {
        TestUtil.test((app, http) -> {
            app.get("/", ctx -> {
                ctx.queryParamsAsClass("param", Integer.class)
                    .check(it -> it.stream().allMatch(number -> number < 5), "All must be smaller than 5")
                    .get();
            });
            var response = http.get("/?param=1&param=2&param=5");
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getBody()).contains("All must be smaller than 5");
        });
    }

    @Test
    public void validator_works_from_java_too() {
        JavalinValidation.register(Instant.class, v -> Instant.ofEpochMilli(Long.parseLong(v)));
        String intString = "123";
        int myInt = Validator.create(Integer.class, intString, "?").get();
        assertThat(myInt).isEqualTo(123);

        Instant fromDate = Validator.create(Instant.class, "1262347200000", "?").get();
        Instant toDate = Validator.create(Instant.class, "1262347300000", "?")
            .check(date -> date.isAfter(fromDate), "'to' has to be after 'from'")
            .get();

        assertThat(toDate.getEpochSecond()).isEqualTo(1262347300L);
        assertThat(Validator.create(Boolean.class, "true", "?").get()).isInstanceOf(Boolean.class);
        assertThat(Validator.create(Double.class, "1.2", "?").get()).isInstanceOf(Double.class);
        assertThat(Validator.create(Float.class, "1.2", "?").get()).isInstanceOf(Float.class);
        assertThat(Validator.create(Integer.class, "123", "?").get()).isInstanceOf(Integer.class);
        assertThat(Validator.create(Long.class, "123", "?").get()).isInstanceOf(Long.class);
    }

}
