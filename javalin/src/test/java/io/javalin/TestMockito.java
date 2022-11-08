package io.javalin;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static io.javalin.http.HttpStatus.IM_A_TEAPOT;
import static io.javalin.http.HttpStatus.OK;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestMockito {

    @Test
    public void shouldCreateMockOfContextFromInterface() {
        Context context = assertDoesNotThrow(() -> mock(Context.class));
        assertThat(context).isNotNull();
    }

    @Test
    public void shouldMockRequestAndResponseBasedMethods() {
        Context context = mock(Context.class);

        AtomicReference<HttpStatus> status = new AtomicReference<>(OK);
        when(context.status(any(HttpStatus.class))).then(value -> {
            status.set(value.getArgument(0, HttpStatus.class));
            return context;
        });
        when(context.status()).then((invocationOnMock) -> status.get());

        assertThat(context.status()).isEqualTo(OK);
        context.status(IM_A_TEAPOT);
        assertThat(context.status()).isEqualTo(IM_A_TEAPOT);
    }

}
