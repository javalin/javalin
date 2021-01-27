package io.javalin.plugin.openapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import io.javalin.http.Handler;
import io.javalin.plugin.openapi.dsl.DocumentedHandler;
import io.javalin.plugin.openapi.dsl.OpenApiDocumentation;

public class TestDocumentedHandler {

    @Test
    public void testInheritance() {
        final DocumentedHandler defaultHandler = new DocumentedHandler(new OpenApiDocumentation(), ctx -> { });
        assertTrue("actual: " + defaultHandler.toString(), defaultHandler.toString().startsWith("io.javalin.plugin.openapi.dsl.DocumentedHandler"));

        final DocumentedHandler customHandler = new CustomDocumentedHandler(new OpenApiDocumentation(), ctx -> {
        });
        assertTrue("actual: " + customHandler.toString(), customHandler.toString().startsWith("io.javalin.plugin.openapi.TestDocumentedHandler$CustomDocumentedHandler"));
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
