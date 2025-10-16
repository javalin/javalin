/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.config.ValidationConfig;
import io.javalin.testing.TestUtil;
import io.javalin.validation.Validation;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.javalin.testing.JavalinTestUtil.exception;
import static io.javalin.testing.JavalinTestUtil.get;
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
            get(app, "/validate", ctx -> ctx.queryParamAsClass("param", Integer.class).get());
            assertThat(http.getBody("/validate?param=hmm")).contains("TYPE_CONVERSION_FAILED");
        });
    }

    @Test
    public void default_values_work_from_java() {
        TestUtil.test((app, http) -> {
            get(app, "/validate", ctx -> ctx.result(ctx.queryParamAsClass("param", Integer.class).getOrDefault(250).toString()));
            assertThat(http.getBody("/validate?param=hmm")).contains("TYPE_CONVERSION_FAILED");
            assertThat(http.getBody("/validate")).isEqualTo("250");
        });
    }

    @Test
    public void get_or_throw_works_from_java() {
        TestUtil.test((app, http) -> {
            get(app, "/", ctx -> {
                Integer myInt = ctx.queryParamAsClass("my-qp", Integer.class)
                    .getOrThrow(e -> new CustomException("'my-qp' is not a number"));
                ctx.result(myInt.toString());
            });
            exception(app, CustomException.class, (e, ctx) -> ctx.result(e.getMessage()));
            assertThat(http.getBody("/")).isEqualTo("'my-qp' is not a number");
        });
    }

    @Test
    public void validator_works_from_java_too() {
        ValidationConfig validationConfig = new ValidationConfig();
        validationConfig.register(Instant.class, v -> Instant.ofEpochMilli(Long.parseLong(v)));
        Validation validation = new Validation(validationConfig);
        String intString = "123";
        int myInt = validation.validator("?", Integer.class, intString).get();
        assertThat(myInt).isEqualTo(123);

        Instant fromDate = validation.validator("?", Instant.class, "1262347200000").get();
        Instant toDate = validation.validator("?", Instant.class, "1262347300000")
            .check(date -> date.isAfter(fromDate), "'to' has to be after 'from'")
            .get();

        assertThat(toDate.getEpochSecond()).isEqualTo(1262347300L);
        assertThat(validation.validator("?", Boolean.class, "true").get()).isInstanceOf(Boolean.class);
        assertThat(validation.validator("?", Double.class, "1.2").get()).isInstanceOf(Double.class);
        assertThat(validation.validator("?", Float.class, "1.2").get()).isInstanceOf(Float.class);
        assertThat(validation.validator("?", Integer.class, "123").get()).isInstanceOf(Integer.class);
        assertThat(validation.validator("?", Long.class, "123").get()).isInstanceOf(Long.class);
    }

}
