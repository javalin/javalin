/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation;

import java.time.Instant;
import org.junit.Test;
import static io.javalin.validation.JavalinValidation.validate;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestValidation_Java {

    @Test
    public void validator_works_from_java_too() {
        JavalinValidation.register(Instant.class, v -> Instant.ofEpochMilli(Long.parseLong(v)));
        String intString = "123";
        int myInt = validate(intString).asClass(Integer.class).getOrThrow();
        assertThat(myInt, is(123));

        String instantString = "1262347200000";
        Instant myInstant = validate(instantString).asClass(Instant.class).getOrThrow();
        assertThat(myInstant.getEpochSecond(), is(1262347200L));

        assertThat(validate("true").asBoolean().getOrThrow(), is(instanceOf(Boolean.class)));
        assertThat(validate("1.2").asDouble().getOrThrow(), is(instanceOf(Double.class)));
        assertThat(validate("1.2").asFloat().getOrThrow(), is(instanceOf(Float.class)));
        assertThat(validate("123").asInt().getOrThrow(), is(instanceOf(Integer.class)));
        assertThat(validate("123").asLong().getOrThrow(), is(instanceOf(Long.class)));
    }

}
