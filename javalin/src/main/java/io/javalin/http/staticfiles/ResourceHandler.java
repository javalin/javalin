package io.javalin.http.staticfiles;

import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ResourceHandler {

    boolean handle(@NotNull HttpServletRequest httpRequest, @NotNull HttpServletResponse httpResponse);

    void addStaticFileConfig(@NotNull StaticFileConfig config);
}
