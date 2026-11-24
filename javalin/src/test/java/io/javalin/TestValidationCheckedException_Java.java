package io.javalin;

import io.javalin.validation.Validation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class TestValidationCheckedException_Java {

    @Test
    void checked_factory_exception_can_be_caught_by_its_type() throws Exception {
        try {
            new Validation().validator("count", Integer.class, "invalid")
                .getOrThrow(errors -> new IOException("Invalid count"));
            fail("Expected the checked exception from the factory");
        } catch (IOException error) {
            assertThat(error).hasMessage("Invalid count");
        }
    }

    @Test
    void valid_value_does_not_invoke_the_exception_factory() throws Exception {
        AtomicBoolean invoked = new AtomicBoolean(false);
        Integer value = new Validation().validator("count", Integer.class, "42")
            .getOrThrow(errors -> {
                invoked.set(true);
                return new IOException("Invalid count");
            });
        assertThat(value).isEqualTo(42);
        assertThat(invoked).isFalse();
    }
}
