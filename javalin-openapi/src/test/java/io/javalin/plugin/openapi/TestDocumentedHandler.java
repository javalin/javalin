package io.javalin.plugin.openapi;

import io.javalin.http.Handler;
import io.javalin.plugin.openapi.dsl.DocumentedHandler;
import io.javalin.plugin.openapi.dsl.OpenApiDocumentation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestDocumentedHandler {

    @Test
    public void testInheritance() {
        final DocumentedHandler defaultHandler = new DocumentedHandler(new OpenApiDocumentation(), ctx -> {
        });
        assertTrue(defaultHandler.toString().startsWith("io.javalin.plugin.openapi.dsl.DocumentedHandler"), "actual: " + defaultHandler.toString());

        final DocumentedHandler customHandler = new CustomDocumentedHandler(new OpenApiDocumentation(), ctx -> {
        });
        assertTrue(customHandler.toString().startsWith("io.javalin.plugin.openapi.TestDocumentedHandler$CustomDocumentedHandler"), "actual: " + customHandler.toString());
    }

    public static class CustomDocumentedHandler extends DocumentedHandler {

        public CustomDocumentedHandler(@NotNull final OpenApiDocumentation documentation, @NotNull final Handler handler) {
            super(documentation, handler);
        }

        @Override
        public String toString() {
            return CustomDocumentedHandler.class.getName() + "@" + Integer.toHexString(hashCode());
        }
    }
}
