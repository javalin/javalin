/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation;

import java.time.Instant;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestValidation_Java {

    @Test
    public void validator_works_from_java_too() {
        String intString = "123";
        int myInt = new Validator(intString).getAs(Integer.class);
        assertThat(myInt, is(123));

        String instantString = "1262347200000";
        Instant myInstant = new Validator(instantString).getAs(Instant.class, v -> Instant.ofEpochMilli(Long.valueOf(v)));
        assertThat(myInstant.getEpochSecond(), is(1262347200L));
    }

}
