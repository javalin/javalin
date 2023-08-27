package io.javalin.http.staticfiles;

import io.javalin.http.Context;

public interface ResourceHandler {
    boolean handle(Context context);

    boolean addStaticFileConfig(StaticFileConfig config);
}
