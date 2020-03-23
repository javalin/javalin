# Contributing

This document describes how to contribute to the core Javalin project, if you want to contribute to the Javalin website you should go to [https://github.com/javalin/javalin.github.io](https://github.com/javalin/javalin.github.io).

## Creating an issue
Remember to include enough information if you're reporting a bug.  
Creating an issue to ask a question is fine.

## Creating a PR
Every PR will be considered.

### How to increase the chance of having your PR merged

* Ask about the feature beforehand (or pick one of the open issues)
* If no issue exists, create an issue for the PR
* Try to write some tests for your change. There are a lot of examples in the `test` dir.
* Format your code so it looks somewhat like the rest of the source 
  (which is formatted using default settings in IntelliJ)

## Running tests

`./mvnw test` (or `mvnw.cmd test` for Windows)

## Building project locally

`./mvnw install` (or `mvnw.cmd install` for Windows)

## Project overview

### Package overview

The following ASCII file structure diagram shows the most important packages and files, with comments.

```
io.javalin
├── apibuilder/                 // Convenience methods for when declaring large APIs
├── core/                       // Things that concern both HTTP and WebSockets
│   ├── security/               // AccessManager, Role interface, security utils
│   ├── util/                   // Misc utils - if you don't know where to place something, this is the place...
│   └── validation/             // Validation for parameters
├── http/                       // Everything related to http requests
│   ├── sse/                    // Server-sent events
│   ├── staticfiles/            // Static file handling
│   ├── util/                   // Misc utils for HTTP, mainly for Context
│   ├── Context.kt              // Wrapper class for request/response, contains everything needed to fulfill a request
│   └── JavalinServlet.kt       // Responsible for the request lifecycle (writes the response to the client)
├── plugin/                     // All plugins (functionality that requires optional dependencies)
│   ├── json/                   // Interfaces for JSON, as well as a Jackson based implementation
│   ├── metrics/                // Metric plugins
│   └── rendering/              // Interface for file rendering, and several template engine implementations
├── websocket/                  // Everything related to WebSockets
│   ├── WsContext.kt            // Wrapper class for WebSockets
│   ├── JavalinWsServlet.kt     // Responsible for WebSocket upgrade, as well as switching between WebSocket and HTTP
│   └── WsHandlerController.kt  // Responsible for Websocket request lifecycle (before, endpoint, after, logging)
└── Javalin.java                // Main public API, responsible for setting up and configuring the server
```

Any public API which takes a `@FunctionalInterface` as a parameter has to be Java 
to achieve the best interoperability between Java and Kotlin. 
Every `@FunctionalInterface` is also Java for the same reasons. 
Almost all tests are written in Kotlin, but it's okay to contribute a Java test.

### Test setup

Almost all tests should be full integration tests. This means every test should

* Start a Javalin instance on a random port
* Attach a handler which does what you want to test
* Perform a HTTP request against that handler
* Verify what you want to test using the response value
* Stop the Javalin instance

This is a lot easier than it sounds. Here is an example test:

```kotlin
@Test
fun `session-cookie is http-only`() = TestUtil.test { app, http ->
    app.get("/store-session") { ctx -> ctx.sessionAttribute("test", "tast") }
    assertThat(http.get("/store-session").headers.getFirst("Set-Cookie").contains("HttpOnly"), `is`(true))
}
```

The `TestUtil.test` function takes care of creating, starting and stopping a Javalin instance (on a random port), 
and provides you with a http-client you can use to perform requests.
You can also give `test` a Javalin instance:

```
TestUtil.test(Javalin.create().configure { ... }) { app, http ->
```

`test` will assign the instance a random port and stop it after the test is done.

## Questions

There's usually someone on [gitter](https://gitter.im/javalin-io/general) who can help you if you have any questions.
