/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.plugin.json.JavalinJackson;
import io.javalin.plugin.openapi.OpenApiOptions;
import io.javalin.plugin.openapi.OpenApiPlugin;
import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import io.javalin.plugin.openapi.ui.ReDocOptions;
import io.javalin.plugin.openapi.ui.SwaggerOptions;
import io.swagger.v3.oas.models.info.Info;

public class HelloWorldSwagger {

    public static void main(String[] args) {

        JavalinJackson.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

        OpenApiOptions openApiOptions = new OpenApiOptions(new Info().version("1.0").description("My Application"))
            .activateAnnotationScanningFor("io.javalin.examples")
            .path("/swagger-docs")
            .swagger(new SwaggerOptions("/swagger").title("My Swagger Documentation"))
            .reDoc(new ReDocOptions("/redoc").title("My ReDoc Documentation"));

        Javalin app = Javalin.create(config -> config.registerPlugin(new OpenApiPlugin(openApiOptions))).start(7070);

        app.post("/users", ExampleController::create);

    }

}

class ExampleController {

    @OpenApi(
        path = "/users",
        method = HttpMethod.POST,
        queryParams = {
            @OpenApiParam(name = "my-query-param")
        },
        responses = {
            @OpenApiResponse(status = "201", returnType = String.class)
        }
    )
    public static void create(Context ctx) {

    }

}
