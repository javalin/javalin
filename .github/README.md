<a name="readme-top"></a>

<div align="center">

  <!--Logo-->
  <a href="https://github.com/javalin/javalin">
    <img src="img/javalin.png" alt="Logo" width="70%">
  </a>

  <!--Title-->
  <h3 align="center">A simple web framework for Java and Kotlin</h3>

  <a href="https://javalin.io/documentation" style="margin: 1em"><strong>View the documentation →</strong></a>

  <!--Badge for CI-->
  <a href="https://github.com/javalin/javalin/actions/workflows/main.yml">
    <img alt="GitHub Workflow Status (with event)" src="https://img.shields.io/github/actions/workflow/status/javalin/javalin/main.yml">
  </a>
  <!--License badge-->
  <a href="https://github.com/javalin/javalin/blob/master/LICENSE">
    <img alt="Static Badge" src="https://img.shields.io/badge/License-Apache_2.0-blue">
  </a>
  <!--Maven central stable version badge-->
  <a href="https://central.sonatype.com/artifact/io.javalin/javalin">
    <img alt="Stable Version" src="https://img.shields.io/maven-central/v/io.javalin/javalin?label=stable">
  </a>
  <!--Reposilite snapshot version badge-->
  <a href="https://repo.reposilite.com/#/snapshots/io/javalin/javalin">
    <img alt="Snapshot Version" src="https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.reposilite.com%2Fsnapshots%2Fio%2Fjavalin%2Fjavalin%2Fmaven-metadata.xml&label=snapshot">
  </a>  
  <!--Discord badge-->
  <a href="https://discord.gg/sgak4e5NKv">
    <img alt="Discord Link" src="https://img.shields.io/badge/discord-javalin-blue?logo=discord&logoColor=white">
  </a>
  <a href="https://discord.gg/sgak4e5NKv">
    <img alt="Discord" src="https://img.shields.io/discord/804862058528505896">
  </a>
  
</div>

# 

Javalin is a very lightweight web framework for Kotlin and Java which supports WebSockets, HTTP2 and async requests. Javalin’s main goals are simplicity, a great developer experience, and first class interoperability between Kotlin and Java.

Javalin is more of a library than a framework. Some key points:
* You don't need to extend anything
* There are no @Annotations
* There is no reflection
* There is no other magic; just code.

#### General information
* The project webpage is [javalin.io](https://javalin.io) (the source is at [javalin/javalin.github.io](https://github.com/javalin/javalin.github.io)).
* Read the documentation on: [javalin.io/documentation](https://javalin.io/documentation)
* A summary of the license can be found at [TLDR Legal](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))
* [Interesting issues](https://github.com/javalin/javalin/issues?q=is%3Aissue+label%3AINFO)

#### Community
We have a very active [Discord](https://discord.gg/sgak4e5NKv) server where you can get help quickly. We also have a (much less active) [Slack](https://join.slack.com/t/javalin-io/shared_invite/zt-1hwdevskx-ftMobDhGxhW0I268B7Ub~w) if you prefer that.

Contributions are very welcome, you can read more about contributing in our guide:
 [CONTRIBUTING](https://github.com/javalin/javalin/contribute)

Please consider [:heart: Sponsoring](https://github.com/sponsors/tipsy) or starring Javalin if you want to support the project.

## Quickstart

### Add dependency

#### Maven

```xml
<dependency>
    <groupId>io.javalin</groupId>
    <artifactId>javalin</artifactId>
    <version>6.0.1</version>
</dependency>
```

#### Gradle

```kotlin
implementation("io.javalin:javalin:6.0.1")
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

You can find more examples in the [javalin-samples](https://github.com/javalin/javalin-samples) repository.

### Api structure and server config
```kotlin
val app = Javalin.create { config ->
    config.http.defaultContentType = "application/json"
    config.http.generateEtags = true
    config.staticFiles.add("/public")
    config.asyncRequestTimeout = 10_000L
    config.compression.brotliAndGzip()
    config.routing.caseInsensitiveRoutes = true
}.routes {
    path("users") {
        get(UserController::getAll)
        post(UserController::create)
        path("{user-id}") {
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
app.ws("/websocket/{path}") { ws ->
    ws.onConnect { ctx -> println("Connected") }
    ws.onMessage { ctx ->
        val user = ctx.message<User>(); // convert from json string to object
        ctx.send(user); //  convert to json string and send back
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
    ctx.uploadedFiles("files").forEach { uploadedFile ->
        FileUtil.streamToFile(uploadedFile.content(), "upload/${uploadedFile.filename()}")
    }
}
```

## Plugins
Javalin has a plugin system that allows you to add functionality to the core library. You can find a list of plugins [here](https://javalin.io/plugins).

Installing a plugin is as easy as adding a dependency to your project and registering it with Javalin:

```kotlin
Javalin.create { config ->
    config.plugins.register(MyPlugin())
}
```

Some of the most popular plugins are:
### OpenAPI Plugin

The [Javalin OpenAPI](https://github.com/javalin/javalin-openapi) plugin allows you to generate an OpenAPI 3.0 specification for your API at compile time.

Annotate your routes with `@OpenApi` to generate the specification:

```kotlin

@OpenApi(
        summary = "Get all users",
        operationId = "getAllUsers",
        tags = ["User"],
        responses = [OpenApiResponse("200", [OpenApiContent(Array<User>::class)])],
        path = "/users",
        methods = [HttpMethod.GET]
    )
    fun getAll(ctx: Context) {
        ctx.json(UserService.getAll())
    }
```

 Swagger UI and ReDoc UI implementations for viewing the generated specification in your browser are also available.

 For more information, see the [Javalin OpenAPI Wiki](https://github.com/javalin/javalin-openapi/wiki).


### Rendering Plugin

The [Javalin Rendering](https://javalin.io/plugins/rendering) plugin allows you to use any template engine with Javalin. 

It includes implementations for JTE, Mustache, Velocity, Pebble, Handlebars, and Thymeleaf, but you also have to add the dependency for the template engine you want to use.

```kotlin
ctx.render("/templateFile.ext", mapOf("firstName" to "John", "lastName" to "Doe"))
```

Once you have included the `javalin-rendering` artifact and one of the supported template engine dependencies in your project, `Context#render` should automatically render your templates. By default, the plugin will look in `src/main/resources/templates` for template files.


### SSL Plugin

The [Javalin SSL](https://javalin.io/plugins/ssl-helpers) plugin allows you to easily configure SSL for your Javalin server, supporting a variety of formats such as PEM, PKCS12, DER, P7B, and JKS.

Enabling SSL on the 443 port is as easy as:

```kotlin
Javalin.create { config ->
    config.plugins.register(SSLPlugin {
        it.pemFromPath("/path/to/cert.pem", "/path/to/key.pem")
    })
}
```

## Sponsors
* [@barbarysoftware](https://github.com/sponsors/barbarysoftware) (50 USD/m)

## Special thanks
* Blake Mizerany, for creating [Sinatra](http://www.sinatrarb.com/)
* Per Wendel, for creating [Spark](http://sparkjava.com/)
* [Christian Rasmussen](https://github.com/chrrasmussen), for being a great guy
* [Per Kristian Kummermo](https://github.com/pkkummermo), also for being a great guy
