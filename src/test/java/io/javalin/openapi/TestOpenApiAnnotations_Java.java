package io.javalin.openapi;

import io.javalin.Javalin;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.JavalinOpenApi;
import io.javalin.plugin.openapi.OpenApiOptions;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaCrudHandler implements CrudHandler {
    @OpenApi(
        responses = {
            @OpenApiResponse(status = "200", returnType = User.class, isArray = true)
        }
    )
    @Override
    public void getAll(@NotNull Context ctx) {
    }

    @OpenApi(
        responses = {
            @OpenApiResponse(status = "200", returnType = User.class)
        }
    )
    @Override
    public void getOne(@NotNull Context ctx, @NotNull String resourceId) {

    }

    @Override
    public void create(@NotNull Context ctx) {

    }

    @Override
    public void update(@NotNull Context ctx, @NotNull String resourceId) {

    }

    @Override
    public void delete(@NotNull Context ctx, @NotNull String resourceId) {

    }
}

class GetAllHandler implements Handler {
    @OpenApi(
        responses = {
            @OpenApiResponse(status = "200", returnType = User.class, isArray = true)
        }
    )
    @Override
    public void handle(@NotNull Context ctx) throws Exception {
    }
}

class GetOneHandler implements Handler {
    @OpenApi(
        responses = {
            @OpenApiResponse(status = "200", returnType = User.class)
        }
    )
    @Override
    public void handle(@NotNull Context ctx) throws Exception {
    }
}

class CreateHandler implements Handler {
    @Override
    public void handle(@NotNull Context ctx) throws Exception {
    }
}

class UpdateHandler implements Handler {
    @Override
    public void handle(@NotNull Context ctx) throws Exception {
    }
}

class DeleteHandler implements Handler {
    @Override
    public void handle(@NotNull Context ctx) throws Exception {
    }
}

public class TestOpenApiAnnotations_Java {
    @Test
    public void testWithSimpleExample() {
        OpenApiOptions openApiOptions = new OpenApiOptions(
            new Info().title("Example").version("1.0.0")
        );
        Javalin app = Javalin.create(config -> config.enableOpenApi(openApiOptions));

        app.get("/users/:user-id", new GetOneHandler());
        app.delete("/users/:user-id", new DeleteHandler());
        app.patch("/users/:user-id", new UpdateHandler());
        app.get("/users", new GetAllHandler());
        app.post("/users", new CreateHandler());

        OpenAPI actual = JavalinOpenApi.createSchema(app);
        assertThat(JsonKt.asJsonString(actual)).isEqualTo(JsonKt.getCrudExampleJson());
    }


    @Test
    public void testWithCrudHandler() {
        OpenApiOptions openApiOptions = new OpenApiOptions(
            new Info().title("Example").version("1.0.0")
        );
        Javalin app = Javalin.create(config -> config.enableOpenApi(openApiOptions));

        app.routes(() -> ApiBuilder.crud("users/:user-id", new JavaCrudHandler()));

        OpenAPI actual = JavalinOpenApi.createSchema(app);
        assertThat(JsonKt.asJsonString(actual)).isEqualTo(JsonKt.getCrudExampleJson());
    }
}
