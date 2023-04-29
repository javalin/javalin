package io.javalin.config;

import jakarta.servlet.http.HttpServletResponse;

public interface JavaLangErrorHandler {
    void handle(HttpServletResponse res, Error err);
}
