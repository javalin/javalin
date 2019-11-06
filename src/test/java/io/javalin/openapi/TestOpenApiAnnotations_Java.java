package io.javalin.openapi;

import io.javalin.Javalin;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.misc.TestLoggingUtilKt;
import io.javalin.plugin.openapi.JavalinOpenApi;
import io.javalin.plugin.openapi.OpenApiOptions;
import io.javalin.plugin.openapi.OpenApiPlugin;
import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JavaCrudHandler implements CrudHandler {
    @OpenApi(
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class, isArray = true))
        }
    )
    @Override
    public void getAll(@NotNull Context ctx) {
    }

    @OpenApi(
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class))
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
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class, isArray = true))
        }
    )
    @Override
    public void handle(@NotNull Context ctx) throws Exception {
    }
}

class GetOneHandler implements Handler {
    @OpenApi(
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class))
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

class JavaMethodReference {
    @OpenApi(
        responses = {@OpenApiResponse(status = "200")}
    )
    public void createHandler(Context ctx) {
    }
}

class ExtendedJavaMethodReference extends JavaMethodReference {
}

class JavaMethodReference2 {
    @OpenApi(
        path = "/test1",
        description = "Test1",
        responses = {@OpenApiResponse(status = "200")}
    )
    public void createHandler1(Context ctx) {
    }

    @OpenApi(
        path = "/test2",
        description = "Test2",
        responses = {@OpenApiResponse(status = "200")}
    )
    public void createHandler2(Context ctx) {
    }
}

class JavaMethodReference3 {
    @OpenApi(
        path = "/test",
        method = HttpMethod.GET,
        description = "Test1",
        responses = {@OpenApiResponse(status = "200")}
    )
    public void createHandler1(Context ctx) {
    }

    @OpenApi(
        path = "/test",
        method = HttpMethod.POST,
        description = "Test2",
        responses = {@OpenApiResponse(status = "200")}
    )
    public void createHandler2(Context ctx) {
    }
}

class JavaStaticMethodReference {
    @OpenApi(
        path = "/test",
        method = HttpMethod.GET,
        responses = {@OpenApiResponse(status = "200")}
    )
    public static void createStaticHandler(Context ctx) {
    }
}

class JavaFieldReference {
    @OpenApi(responses = {@OpenApiResponse(status = "200")})
    public static Handler handler = new Handler() {
        @Override
        public void handle(@NotNull Context ctx) throws Exception {
        }
    };
}

class ClassHandlerWithInvalidPath {
    @OpenApi(
        method = HttpMethod.GET,
        path = "/account", // /account/:id would be correct
        pathParams = @OpenApiParam(name = "id", type = Integer.class)
    )
    void getOne(Context ctx) {
    }

    @OpenApi(
        method = HttpMethod.GET,
        path = "/account"
    )
    void getAll(Context ctx) {
    }
}

public class TestOpenApiAnnotations_Java {
    @Test
    public void testWithSimpleExample() {
        OpenApiOptions openApiOptions = new OpenApiOptions(
            new Info().title("Example").version("1.0.0")
        );
        Javalin app = Javalin.create(config -> config.registerPlugin(new OpenApiPlugin(openApiOptions)));

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
        Javalin app = Javalin.create(config -> config.registerPlugin(new OpenApiPlugin(openApiOptions)));

        app.routes(() -> ApiBuilder.crud("users/:user-id", new JavaCrudHandler()));

        OpenAPI actual = JavalinOpenApi.createSchema(app);
        assertThat(JsonKt.asJsonString(actual)).isEqualTo(JsonKt.getCrudExampleJson());
    }

    @Test
    public void testWithJavaMethodReference() {
        Info info = new Info().title("Example").version("1.0.0");
        OpenApiOptions options = new OpenApiOptions(info);

        OpenAPI schema = OpenApiTestUtils.extractSchemaForTest(options, app -> {
            app.get("/test", new JavaMethodReference()::createHandler);
            return Unit.INSTANCE;
        });
        OpenApiTestUtils.assertEqualTo(schema, JsonKt.getSimpleExample());
    }

    @Test
    public void testWithExtendedJavaMethodReference() {
        Info info = new Info().title("Example").version("1.0.0");
        OpenApiOptions options = new OpenApiOptions(info);

        OpenAPI schema = OpenApiTestUtils.extractSchemaForTest(options, app -> {
            app.get("/test", new ExtendedJavaMethodReference()::createHandler);
            return Unit.INSTANCE;
        });
        OpenApiTestUtils.assertEqualTo(schema, JsonKt.getSimpleExample());
    }

    @Test
    public void testWithJavaMethodReferenceAndMultipleMethods() {
        Info info = new Info().title("Example").version("1.0.0");
        OpenApiOptions options = new OpenApiOptions(info);

        OpenAPI schema = OpenApiTestUtils.extractSchemaForTest(options, app -> {
            JavaMethodReference2 ref = new JavaMethodReference2();
            app.get("/test1", ref::createHandler1);
            app.get("/test2", ref::createHandler2);
            return Unit.INSTANCE;
        });
        OpenApiTestUtils.assertEqualTo(schema, JsonKt.getSimpleExampleWithMultipleGets());
    }

    @Test
    public void testWithJavaMethodReferenceAndMultipleMethodsAndSamePath() {
        Info info = new Info().title("Example").version("1.0.0");
        OpenApiOptions options = new OpenApiOptions(info);

        OpenAPI schema = OpenApiTestUtils.extractSchemaForTest(options, app -> {
            JavaMethodReference3 ref = new JavaMethodReference3();
            app.get("/test", ref::createHandler1);
            app.post("/test", ref::createHandler2);
            return Unit.INSTANCE;
        });
        OpenApiTestUtils.assertEqualTo(schema, JsonKt.getSimpleExampleWithMultipleHttpMethods());
    }

    @Test
    public void testWithJavaStaticMethodReference() {
        Info info = new Info().title("Example").version("1.0.0");
        OpenApiOptions options = new OpenApiOptions(info)
            .activateAnnotationScanningFor("io.javalin.openapi");

        OpenAPI schema = OpenApiTestUtils.extractSchemaForTest(options, app -> {
            app.get("/test", JavaStaticMethodReference::createStaticHandler);
            return Unit.INSTANCE;
        });
        OpenApiTestUtils.assertEqualTo(schema, JsonKt.getSimpleExample());
    }

    @Test
    public void testWithJavaFieldReference() {
        OpenAPI schema = OpenApiTestUtils.extractSchemaForTest(app -> {
            app.get("/test", JavaFieldReference.handler);
            return Unit.INSTANCE;
        });
        OpenApiTestUtils.assertEqualTo(schema, JsonKt.getSimpleExample());
    }

    @Test
    public void testIfUserIsWarnedOnInvalidPath() {
        String log = TestLoggingUtilKt.captureStdOut(() -> {
            OpenApiTestUtils.extractSchemaForTest(app -> {
                ClassHandlerWithInvalidPath handler = new ClassHandlerWithInvalidPath();
                app.get("/account", handler::getAll);
                app.get("/account/:id", handler::getOne);
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });
        assertThat(log).contains(
            "The `path` of one of the @OpenApi annotations on io.javalin.openapi.ClassHandlerWithInvalidPath is incorrect. " +
                "The path param \":id\" is documented, but couldn't be found in GET \"/account\". " +
                "Do you mean GET \"/account/:id\"?"
        );
    }
}
