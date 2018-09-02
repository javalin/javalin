[![Chat at https://gitter.im/javalin-io/general](https://badges.gitter.im/javalin-io/general.svg)](https://gitter.im/javalin-io/general)
[![Travis](https://img.shields.io/travis/tipsy/javalin.svg)](https://travis-ci.org/tipsy/javalin/builds)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven](https://img.shields.io/maven-central/v/io.javalin/javalin.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22io.javalin%22%20AND%20a%3A%22javalin%22)

# Javalin - A simple web framework for Java and Kotlin

The project webpage is [javalin.io](https://javalin.io).

Documentation: [javalin.io/documentation](https://javalin.io/documentation)

Chatroom: https://gitter.im/javalin-io/general

Contributions are very welcome: [CONTRIBUTING.md](https://github.com/tipsy/javalin/blob/master/CONTRIBUTING.md)

## Java quickstart

### Add dependency (maven)

```xml
<dependency>
    <groupId>io.javalin</groupId>
    <artifactId>javalin</artifactId>
    <version>2.1.1</version>
</dependency>
```

### Start programming:

```java
import io.javalin.Javalin;

public class HelloWorld {
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7000);
        app.get("/", ctx -> ctx.result("Hello World"));
    }
}
```

## Kotlin quickstart

### Add dependency (gradle)
```kotlin
compile 'io.javalin:javalin:2.1.1'
```

### Start programming
```kotlin
import io.javalin.Javalin

fun main(args: Array<String>) {
    val app = Javalin.create().start(7000)
    app.get("/") { ctx -> ctx.result("Hello World") }
}
```

## Examples
This section contains a few examples, mostly just extracted from the [docs](https://javalin.io/documentation).
All examples are in Kotlin, but you can find them in Java in the documentation (it's just syntax changes).

### Api structure and server config
```kotlin
val app = Javalin.create().apply {
    enableCorsForAllOrigins()
    enableStaticFiles("/public")
    enableStaticFiles("uploads", Location.EXTERNAL)
}.start(port)

app.routes {
    path("users") {
        get(UserController::getAll)
        post(UserController::create)
        path(":user-id") {
            get(UserController::getOne)
            patch(UserController::update)
            delete(UserController::delete)
        }
    }
}
```

### WebSockets
```kotlin
app.ws("/websocket") { ws ->
    ws.onConnect { session -> println("Connected") }
    ws.onMessage { session, message ->
        println("Received: " + message)
        session.remote.sendString("Echo: " + message)
    }
    ws.onClose { session, statusCode, reason -> println("Closed") }
    ws.onError { session, throwable -> println("Errored") }
}
```

### Filters and Mappers
```kotlin
app.before("/some-path/*") { ctx ->  ... } // runs before requests to /some-path/*
app.before { ctx -> ... } // runs before all requests
app.after { ctx -> ... } // runs after all requests
app.exception(Exception.class) { e, ctx -> ... } // runs if uncaught Exception
app.error(404) { ctx -> ... } // runs if status is 404 (after all other handlers)
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

## Special thanks
* Blake Mizerany, for creating [Sinatra](http://www.sinatrarb.com/)
* Per Wendel, for creating [Spark](http://sparkjava.com/)
* [Christian Rasmussen](https://github.com/chrrasmussen), for being a great guy
* [Per Kristian Kummermo](https://github.com/pkkummermo), also for being a great guy
