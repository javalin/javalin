package io.javalin.http.staticfiles;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

public interface ResourceHandler {
    boolean handle(HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    boolean addStaticFileConfig(StaticFileConfig config);
}
