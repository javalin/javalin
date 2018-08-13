# Contributing

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

`mvn test`

## Building project locally

`mvn install`

## Project overview

Javalin is a fairly small project (less than 4k LoC, including comments).

### Package overview

The following ASCII file structure diagram shows the most important packages and files, with comments.

```
io.javalin
├── apibuilder/               // Convenience methods for when you're creating applications with a lot of routes
├── core/
│   ├── util/
│   │   ├── JettyServerUtil   // Responsible for setting up the Jetty Server
│   ├── JavalinServlet.kt     // Responsible for the request lifecycle (writes the response to the client)
│   └── PathMatcher.kt        // Responsible for matching requests and parsing URLs
├── json/                     // JSON mapping (functional interfaces + optional Jackson implementation)
├── rendering/                // Template engines and markdown renderer
├── security/                 // All functionality related to access management for routes
├── staticfiles/              // All functionality related to static files
├── websocket/
│   ├── WsSession.kt          // Similar to Context.kt, but for WebSockets
│   └── WsPathMatcher.kt      // Similar to PathMatcher.kt, but for WebSockets
├── Context.kt                // Wrapper class for request/reponse, contains everything needed to fulfill a request
└── Javalin.java              // Main public API, responsible for setting up and configuring the server
```

Most of the codebase is Kotlin (~2.5k lines), and the rest is Java (~1k lines). 
Any public API which takes a `@FunctionalInterface` as a parameter has to be Java 
to achieve the best interoperability between Java and Kotlin. 
Every `@FunctionalInterface` is also Java for the same reasons. 
All tests are written in Kotlin.

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
TestUtil.test(Javalin.create().dontIgnoreTrailingSlashes()) { app, http ->
```

`test` will assign the instance a random port and stop it after the test is done.

## Questions

There's usually someone on [gitter](https://gitter.im/javalin-io/general) who can help you if you have any questions.
