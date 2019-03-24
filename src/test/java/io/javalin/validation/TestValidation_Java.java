/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation;

import java.time.Instant;
import org.junit.Test;
import static io.javalin.validation.JavalinValidation.validate;
import static org.assertj.core.api.Assertions.assertThat;

public class TestValidation_Java {

    @Test
    public void validator_works_from_java_too() {
        JavalinValidation.register(Instant.class, v -> Instant.ofEpochMilli(Long.parseLong(v)));
        String intString = "123";
        int myInt = validate(intString).asClass(Integer.class).get();
        assertThat(myInt).isEqualTo(123);

        Instant fromDate = validate("1262347200000").asClass(Instant.class).get();
        Instant toDate = validate("1262347300000").asClass(Instant.class)
            .check(it -> it.isAfter(fromDate), "'to' has to be after 'from'")
            .get();

        assertThat(toDate.getEpochSecond()).isEqualTo(1262347300L);

        assertThat(validate("true").asBoolean().get()).isInstanceOf(Boolean.class);
        assertThat(validate("1.2").asDouble().get()).isInstanceOf(Double.class);
        assertThat(validate("1.2").asFloat().get()).isInstanceOf(Float.class);
        assertThat(validate("123").asInt().get()).isInstanceOf(Integer.class);
        assertThat(validate("123").asLong().get()).isInstanceOf(Long.class);
    }

}
