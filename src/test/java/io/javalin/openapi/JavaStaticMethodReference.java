package io.javalin.openapi;

import io.javalin.http.Context;
import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;

class JavaStaticMethodReference {
    @OpenApi(
        path = "/test",
        method = HttpMethod.GET,
        responses = {@OpenApiResponse(status = "200")}
    )
    public static void createStaticHandler(Context ctx) {
    }
}
