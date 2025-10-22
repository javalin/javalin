package io.javalin;

import io.javalin.apibuilder.ApiBuilder;
import io.javalin.config.Key;
import io.javalin.http.*;
import io.javalin.plugin.ContextPlugin;
import io.javalin.plugin.bundled.CorsPlugin;
import io.javalin.validation.ValidationError;
import io.javalin.validation.Validator;
import io.javalin.websocket.WsConfig;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static io.javalin.testing.JavalinTestUtil.after;
import static io.javalin.testing.JavalinTestUtil.before;
import static io.javalin.testing.JavalinTestUtil.exception;
import static io.javalin.testing.JavalinTestUtil.get;
import static io.javalin.testing.JavalinTestUtil.sse;
import static io.javalin.testing.JavalinTestUtil.ws;
import static io.javalin.testing.JavalinTestUtil.wsAfter;
import static io.javalin.testing.JavalinTestUtil.wsException;

// @formatter:off
public class TestPublicApi_Java {
    static public class TestContextPlugin extends ContextPlugin<Void, HandlerType> {
        public HandlerType createExtension(Context context) {
            return context.method();
        }
    }

    public static void main(String[] args) {
        Javalin.create(config -> {
            config.routes.get("/", ctx -> ctx.result("Hello World"));
        }).start(7070);
        var testComponentkey = new Key<String>("test-component");
        var app = Javalin.create(config -> {
            config.appData(testComponentkey, "name");
            config.validation.register(Instant.class, v -> Instant.ofEpochMilli(Long.parseLong(v)));
            config.registerPlugin(new CorsPlugin(cors -> {
                cors.addRule(rule -> {
                    rule.path = "images*";
                    rule.allowHost("https://images.local");
                });
            }));
            config.registerPlugin(new TestContextPlugin());
            config.http.asyncTimeout = 10_000L;
            config.routes.apiBuilder(() -> {
                ApiBuilder.path("users", () -> {
                    ApiBuilder.get(UserController::getAll);
                    ApiBuilder.post(UserController::create);
                    ApiBuilder.path("{userId}", () -> {
                        ApiBuilder.get(UserController::getOne);
                        ApiBuilder.patch(UserController::update);
                        ApiBuilder.delete(UserController::delete);
                    });
                    ApiBuilder.ws("events", UserController::webSocketEvents);
                });
            });
        });
        before(app, ctx -> {});
        before(app, "/path/*", ctx -> {});
        after(app, ctx -> {});
        after(app, "/path/*", ctx -> {});

        // validation
        get(app, "/", ctx -> {
            Integer myValue = ctx.queryParamAsClass("value", Integer.class).getOrDefault(788);
            ctx.result("" + myValue);

            Instant fromDate = ctx.queryParamAsClass("from", Instant.class).get();
            Instant toDate = ctx.queryParamAsClass("to", Instant.class)
                .check(it -> it.isAfter(fromDate), "'to' has to be after 'from'")
                .get();

            MyObject myObject = ctx.bodyValidator(MyObject.class)
                .check(obj -> obj.myObjectProperty == 2, "THINGS_MUST_BE_EQUAL")
                .get();

            ctx.queryParamAsClass("param", Integer.class)
                .check(it -> it > 5, new ValidationError<>("OVER_LIMIT", Map.of("limit", 5)))
                .get();

            Validator<Integer> ageValidator = ctx.queryParamAsClass("age", Integer.class)
                .check(n -> n < 4, "TOO_YOUNG");

            Map<String, List<ValidationError<Integer>>> errors = ageValidator.errors();

        });

        exception(app, NullPointerException.class, (e, ctx) -> { /* ... */ });
        exception(app, Exception.class, (e, ctx) -> { /* ... */ });
        wsException(app, NullPointerException.class, (e, ctx) -> { /* ... */ });
        wsException(app, Exception.class, (e, ctx) -> { /* ... */ });

        sse(app, "/sse", client -> {
            client.sendEvent("connected", "Hello, SSE");
            client.onClose(() -> System.out.println("Client disconnected"));
            client.close(); // close the client
        });

        Javalin.create(config -> {
            config.requestLogger.http((ctx, ms) -> {
                // log things here
            });
        });

        Javalin.create(config -> {
            Consumer<WsConfig> requestLogger = ws -> {};
            config.routes.ws("/path", requestLogger);
        });

        Javalin.create(config -> {
            config.events.serverStarting(() -> { });
            config.events.serverStarted(() -> { });
            config.events.serverStartFailed(() -> { });
            config.events.serverStopping(() -> { });
            config.events.serverStopped(() -> { });
            config.events.handlerAdded(handlerMetaInfo -> { });
            config.events.wsHandlerAdded(wsHandlerMetaInfo -> { });
        });

        ws(app, "/websocket/{path}", ws -> {
            ws.onConnect(ctx -> System.out.println("Connected"));
            ws.onMessage(ctx -> {
                User user = ctx.messageAsClass(User.class); // convert from json
                ctx.send(user); // convert to json and send back
            });
            ws.onBinaryMessage(ctx -> System.out.println("Message"));
            ws.onClose(ctx -> System.out.println("Closed"));
            ws.onError(ctx -> System.out.println("Errored"));
        });

        wsAfter(app, ws -> { });
        wsAfter(app, "/path/*", ws -> { });

        // context
        get(app, "/", ctx -> {
            ctx.body();
            ctx.bodyAsBytes();
            ctx.bodyAsClass(Integer.class);
            ctx.bodyStreamAsClass(Integer.class);
            ctx.bodyValidator(Integer.class);
            ctx.bodyInputStream();
            ctx.uploadedFile("name");
            ctx.uploadedFiles("name");
            ctx.uploadedFiles();
            ctx.uploadedFileMap();
            ctx.formParam("name");
            ctx.formParamAsClass("name",Integer.class);
            ctx.formParams("name");
            ctx.formParamMap();
            ctx.pathParam("name");
            ctx.pathParamAsClass("name",Integer.class);
            ctx.pathParamMap();
            ctx.basicAuthCredentials();
            ctx.attribute("name",Integer.class);
            ctx.attribute("name");
            ctx.attributeOrCompute("name", ctx2-> "");
            ctx.attributeMap();
            ctx.contentLength();
            ctx.contentType();
            ctx.cookie("name");
            ctx.cookieMap();
            ctx.header("name");
            ctx.headerAsClass("name",Integer.class);
            ctx.headerMap();
            ctx.host();
            ctx.ip();
            ctx.isMultipart();
            ctx.isMultipartFormData();
            ctx.method();
            ctx.path();
            ctx.port();
            ctx.protocol();
            ctx.queryParam("name");
            ctx.queryParamAsClass("name",Integer.class);
            ctx.queryParams("name");
            ctx.queryParamMap();
            ctx.queryString();
            ctx.scheme();
            ctx.sessionAttribute("name",Integer.class);
            ctx.sessionAttribute("name");
            ctx.consumeSessionAttribute("name");
            ctx.cachedSessionAttribute("name",Integer.class);
            ctx.cachedSessionAttribute("name");
            ctx.cachedSessionAttributeOrCompute("name", ctx2-> "");
            ctx.sessionAttributeMap();
            ctx.url();
            ctx.fullUrl();
            ctx.contextPath();
            ctx.userAgent();
            ctx.req();
            ctx.result("result");
            ctx.result(new byte[0]);
            ctx.result(ctx.resultInputStream());
            ctx.future(() -> CompletableFuture.supplyAsync(() -> "result"));
            ctx.writeSeekableStream(ctx.resultInputStream(), ContentType.APPLICATION_BZ.getMimeType());
            ctx.result();
            ctx.resultInputStream();
            ctx.contentType("type");
            ctx.header("name","value");
            ctx.redirect("/path",HttpStatus.FOUND);
            ctx.status(HttpStatus.FOUND);
            ctx.status();
            ctx.cookie("name","value",2);
            ctx.cookie(new Cookie("name","value","/path", 2, true));
            ctx.removeCookie("name","/path");
            ctx.json(new User());
            ctx.jsonStream(new User());
            ctx.html("html");
            ctx.render("/template.tmpl",Map.of("test", "tast"));
            ctx.res();
            ctx.async(() -> {});
            ctx.appData(testComponentkey);
            ctx.endpoint();
            ctx.cookieStore();
            ctx.with(TestContextPlugin.class);
        });
    }


    static class UserController {
        public static void getAll(Context ctx) { /* ... */ }
        public static void create(Context ctx) { /* ... */ }
        public static void getOne(Context ctx) { /* ... */ }
        public static void update(Context ctx) { /* ... */ }
        public static void delete(Context ctx) { /* ... */ }
        public static void webSocketEvents(WsConfig wsConfig) { /* ... */ }
    }

    private static class MyObject {
        public int myObjectProperty;
    }

    private static class User { }
}
// @formatter:on
