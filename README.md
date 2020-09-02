[![Chat at https://gitter.im/javalin-io/general](https://badges.gitter.im/javalin-io/general.svg)](https://gitter.im/javalin-io/general)
[![Travis](https://github.com/tipsy/javalin/workflows/Test%20all%20JDKs%20on%20all%20OSes/badge.svg)](https://github.com/tipsy/javalin/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven](https://img.shields.io/maven-central/v/io.javalin/javalin.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22io.javalin%22%20AND%20a%3A%22javalin%22)

# Javalin - A simple web framework for Java and Kotlin

Javalin is a very lightweight web framework for Kotlin and Java which supports WebSockets, HTTP2 and async requests. Javalin’s main goals are simplicity, a great developer experience, and first class interoperability between Kotlin and Java.

Javalin is more of a library than a framework. Some key points:
* You don't need to extend anything
* There are no @Annotations
* There is no reflection
* There is no other magic; just code.

General information:
* The project webpage is [javalin.io](https://javalin.io) (repo for webpage is at [github.com/javalin/javalin.github.io](https://github.com/javalin/javalin.github.io)).
* Documentation: [javalin.io/documentation](https://javalin.io/documentation)
* Chat: https://gitter.im/javalin-io/general
* Contributions are very welcome: [CONTRIBUTING.md](https://github.com/tipsy/javalin/blob/master/CONTRIBUTING.md)
* License summary: https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)
* Interesting issues: [/tipsy/javalin/issues?q=label:INFO](https://github.com/tipsy/javalin/issues?q=is%3Aissue+label%3AINFO)

## Quickstart

### Add dependency

#### Maven

```xml
<dependency>
    <groupId>io.javalin</groupId>
    <artifactId>javalin</artifactId>
    <version>3.10.1</version>
</dependency>
```

#### Gradle

```groovy
compile "io.javalin:javalin:3.10.1"
```

### Start programming (Java)

```java
import io.javalin.Javalin;

public class HelloWorld {
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7000);
        app.get("/", ctx -> ctx.result("Hello World"));
    }
}
```

### Start programming (Kotlin)
```kotlin
import io.javalin.Javalin

fun main() {
    val app = Javalin.create().start(7000)
    app.get("/") { ctx -> ctx.result("Hello World") }
}
```

## Examples
This section contains a few examples, mostly just extracted from the [docs](https://javalin.io/documentation).
All examples are in Kotlin, but you can find them in Java in the documentation (it's just syntax changes).

### Api structure and server config
```kotlin
val app = Javalin.create { config ->
    config.defaultContentType = "application/json"
    config.autogenerateEtags = true
    config.addStaticFiles("/public")
    config.asyncRequestTimeout = 10_000L
    config.dynamicGzip = true
    config.enforceSsl = true
}.routes {
    path("users") {
        get(UserController::getAll)
        post(UserController::create)
        path(":user-id") {
            get(UserController::getOne)
            patch(UserController::update)
            delete(UserController::delete)
        }
        ws("events", userController::webSocketEvents)
    }
}.start(port)
```

### WebSockets
```kotlin
app.ws("/websocket/:path") { ws ->
    ws.onConnect { ctx -> println("Connected") }
    ws.onMessage { ctx ->
        val user = ctx.message<User>(); // convert from json string to object
        ctx.send(user); // convert to json string and send back
    }
    ws.onClose { ctx -> println("Closed") }
    ws.onError { ctx -> println("Errored") }
}
```

### Filters and Mappers
```kotlin
app.before("/some-path/*") { ctx ->  ... } // runs before requests to /some-path/*
app.before { ctx -> ... } // runs before all requests
app.after { ctx -> ... } // runs after all requests
app.exception(Exception.class) { e, ctx -> ... } // runs if uncaught Exception
app.error(404) { ctx -> ... } // runs if status is 404 (after all other handlers)

app.wsBefore("/some-path/*") { ws ->  ... } // runs before ws events on /some-path/*
app.wsBefore { ws -> ... } // runs before all ws events
app.wsAfter { ws -> ... } // runs after all ws events
app.wsException(Exception.class) { e, ctx -> ... } // runs if uncaught Exception in ws handler
```

### JSON-mapping
```kotlin
var todos = arrayOf(...)
app.get("/todos") { ctx -> // map array of Todos to json-string
    ctx.json(todos)
}
app.put("/todos") { ctx -> // map request-body (json) to array of Todos
    todos = ctx.body<Array<Todo>>()
    ctx.status(204)
}
```

### File uploads
```kotlin
app.post("/upload") { ctx ->
    ctx.uploadedFiles("files").forEach { (contentType, content, name, extension) ->
        FileUtil.streamToFile(content, "upload/$name")
    }
}
```

### OpenAPI (Swagger)

Javalin has an OpenAPI (Swagger) plugin. Documentation can be enabled both through a DSL and through annotations,
and Javalin can render docs using both SwaggerUI and ReDoc. Read more at https://javalin.io/plugins/openapi.

## Special thanks
* Blake Mizerany, for creating [Sinatra](http://www.sinatrarb.com/)
* Per Wendel, for creating [Spark](http://sparkjava.com/)
* [Christian Rasmussen](https://github.com/chrrasmussen), for being a great guy
* [Per Kristian Kummermo](https://github.com/pkkummermo), also for being a great guy
