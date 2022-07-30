package io.javalin.testing;

import io.javalin.http.Context;
import io.javalin.http.HttpCode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyInt;
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

        AtomicInteger status = new AtomicInteger(HttpCode.OK.getStatus());
        when(context.status(anyInt())).then(value -> {
            status.set(value.getArgument(0, Integer.class));
            return context;
        });
        when(context.statusCode()).then((invocationOnMock) -> status.get());

        assertThat(context.statusCode()).isEqualTo(HttpCode.OK.getStatus());
        context.status(HttpCode.IM_A_TEAPOT.getStatus());
        assertThat(context.statusCode()).isEqualTo(HttpCode.IM_A_TEAPOT.getStatus());
    }

}
