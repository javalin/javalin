package io.javalin.plugin.openapi;

import io.javalin.Javalin;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;
import io.javalin.http.Handler;
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
import static io.javalin.testing.TestLoggingUtil.captureStdOut;
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
        method = HttpMethod.GET,
        responses = {@OpenApiResponse(status = "200")}
    )
    public static void createStaticHandler(Context ctx) {
    }
}

class JavaStaticFieldReference {
    @OpenApi(responses = {@OpenApiResponse(status = "200")})
    public static Handler handler = new Handler() {
        @Override
        public void handle(@NotNull Context ctx) throws Exception {
        }
    };
}

class JavaFieldReference {
    public final int irrelevantField = 0;

    @OpenApi(
        responses = {@OpenApiResponse(status = "200")})
    public final Handler handler = new Handler() {
        @Override
        public void handle(@NotNull Context ctx) throws Exception {
        }
    };
}

class JavaLambdaFieldReference {
    @OpenApi(
        responses = {@OpenApiResponse(status = "200")})
    public final Handler handler = ctx -> { };
}

class JavaMultipleFieldReferences {
    @OpenApi(
        path = "/test1", // parameter needed to due ambiguity
        responses = {@OpenApiResponse(status = "200")})
    public final Handler handler1 = ctx -> { };

    @OpenApi(
        path = "/test2", // parameter needed to due ambiguity
        responses = {@OpenApiResponse(status = "200")})
    public final Handler handler2 = ctx -> { };

    @OpenApi(
        method = HttpMethod.PUT,
        responses = {@OpenApiResponse(status = "200")})
    public final Handler putHandler = ctx -> { };

    @OpenApi(
        method = HttpMethod.DELETE,
        responses = {@OpenApiResponse(status = "200")})
    public final Handler deleteHandler = ctx -> { };
}

class JavaStaticLambdaFieldReference {
    @OpenApi(
        path = "/test",
        method = HttpMethod.GET,
        responses = {@OpenApiResponse(status = "200")})
    public static final Handler handler = ctx -> { };
}

class CustomOuterClassHandler implements Handler {
    public final int irrelevantField;

    CustomOuterClassHandler() {
        irrelevantField = 2;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        // some custom user code
    }
}

class JavaOuterClassFieldReference {
    @OpenApi(responses = {@OpenApiResponse(status = "200")})
    public final Handler handler = new CustomOuterClassHandler(){};
    // N.B. curly brackets '{}' are important to make the above a pseudo-anonymous class
}

class JavaInnerClassFieldReference {
    @OpenApi(responses = {@OpenApiResponse(status = "200")})
    public final Handler handler = new CustomInnerClassHandler(ctx -> {});

    private class CustomInnerClassHandler implements Handler {
        public final int irrelevantField;
        public final Handler userHandler;

        public CustomInnerClassHandler(final Handler userHandler) {
            this.userHandler = userHandler;
            irrelevantField = 2;
        }

        @Override
        public void handle(@NotNull Context ctx) throws Exception {
            // derived custom handler
        }
    };
}

class JavaAnonymousClassFieldReference {
    @OpenApi(responses = {@OpenApiResponse(status = "200")})
    public final Handler handler = new Handler() {
        @Override
        public void handle(@NotNull Context ctx) throws Exception {
            // derived custom handler
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
            .activateAnnotationScanningFor("io.javalin.plugin.openapi");

        OpenAPI schema = OpenApiTestUtils.extractSchemaForTest(options, app -> {
            app.get("/test", JavaStaticMethodReference::createStaticHandler);
            return Unit.INSTANCE;
        });
        OpenApiTestUtils.assertEqualTo(schema, JsonKt.getSimpleExampleWithDescription());
    }

    @Test
    public void testWithStaticJavaFieldReference() {
        OpenAPI schema = OpenApiTestUtils.extractSchemaForTest(app -> {
            app.get("/test", JavaStaticFieldReference.handler);
            return Unit.INSTANCE;
        });
        OpenApiTestUtils.assertEqualTo(schema, JsonKt.getSimpleExample());
    }

    @Test
    public void testWithJavaFieldReference() {
        final JavaFieldReference instance = new JavaFieldReference();
        OpenAPI schema = OpenApiTestUtils.extractSchemaForTest(app -> {
            app.get("/test", instance.handler);
            return Unit.INSTANCE;
        });
        OpenApiTestUtils.assertEqualTo(schema, JsonKt.getSimpleExample());
    }

    @Test
    public void testWithJavaLambdaFieldReference() {
        final JavaLambdaFieldReference instance = new JavaLambdaFieldReference();
        OpenAPI schema = OpenApiTestUtils.extractSchemaForTest(app -> {
            app.get("/test", instance.handler);
            return Unit.INSTANCE;
        });
        OpenApiTestUtils.assertEqualTo(schema, JsonKt.getSimpleExample());
    }

    @Test
    public void testWithMultipleFieldReferences() {
        final JavaMultipleFieldReferences instance = new JavaMultipleFieldReferences();

        OpenAPI schemaPut = OpenApiTestUtils.extractSchemaForTest(app -> {
            app.put("/put", instance.putHandler);
            return Unit.INSTANCE;
        });
        OpenApiTestUtils.assertEqualTo(schemaPut, JsonKt.getSimplePutExample());

        OpenAPI schemaDelete = OpenApiTestUtils.extractSchemaForTest(app -> {
            app.put("/delete", instance.deleteHandler);
            return Unit.INSTANCE;
        });
        OpenApiTestUtils.assertEqualTo(schemaDelete, JsonKt.getSimpleDeleteExample());

        OpenAPI schemaTestCase1 = OpenApiTestUtils.extractSchemaForTest(app -> {
            app.get("/test1", instance.handler1);
            return Unit.INSTANCE;
        });
        OpenApiTestUtils.assertEqualTo(schemaTestCase1, JsonKt.getSimpleExample1());

        OpenAPI schemaTestCase2 = OpenApiTestUtils.extractSchemaForTest(app -> {
            app.get("/test2", instance.handler2);
            return Unit.INSTANCE;
        });
        OpenApiTestUtils.assertEqualTo(schemaTestCase2, JsonKt.getSimpleExample2());
    }


    @Test
    public void testWithJavaOuterClassFieldReference() {
        final JavaOuterClassFieldReference instance = new JavaOuterClassFieldReference();
        OpenAPI schema = OpenApiTestUtils.extractSchemaForTest(app -> {
            app.get("/test", instance.handler);
            return Unit.INSTANCE;
        });
        OpenApiTestUtils.assertEqualTo(schema, JsonKt.getSimpleExample());
    }

    @Test
    public void testWithJavaInnerClassFieldReference() {
        final JavaInnerClassFieldReference instance = new JavaInnerClassFieldReference();
        OpenAPI schema = OpenApiTestUtils.extractSchemaForTest(app -> {
            app.get("/test", instance.handler);
            return Unit.INSTANCE;
        });
        OpenApiTestUtils.assertEqualTo(schema, JsonKt.getSimpleExample());
    }

    @Test
    public void testWithAnonymousJavaClassFieldReference() {
        final JavaAnonymousClassFieldReference instance = new JavaAnonymousClassFieldReference();
        OpenAPI schema = OpenApiTestUtils.extractSchemaForTest(app -> {
            app.get("/test", instance.handler);
            return Unit.INSTANCE;
        });
        OpenApiTestUtils.assertEqualTo(schema, JsonKt.getSimpleExample());
    }

    @Test
    public void testWithJavaStaticLambdaFieldReference() {
        OpenAPI schema = OpenApiTestUtils.extractSchemaForTest(app -> {
            app.get("/test", JavaStaticLambdaFieldReference.handler);
            return Unit.INSTANCE;
        });
        OpenApiTestUtils.assertEqualTo(schema, JsonKt.getSimpleExample());
    }

    @Test
    public void testIfUserIsWarnedOnInvalidPath() {
        String log = captureStdOut(() ->
            OpenApiTestUtils.extractSchemaForTest(app -> {
                ClassHandlerWithInvalidPath handler = new ClassHandlerWithInvalidPath();
                app.get("/account", handler::getAll);
                app.get("/account/:id", handler::getOne);
                return Unit.INSTANCE;
            })
        );
        assertThat(log).contains(
            "The `path` of one of the @OpenApi annotations on io.javalin.plugin.openapi.ClassHandlerWithInvalidPath is incorrect. " +
                "The path param \":id\" is documented, but couldn't be found in GET \"/account\". " +
                "Do you mean GET \"/account/:id\"?"
        );
    }
}
