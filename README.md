[![Chat at https://gitter.im/javalin-io/general](https://badges.gitter.im/javalin-io/general.svg)](https://gitter.im/javalin-io/general)
[![Travis](https://img.shields.io/travis/tipsy/javalin.svg)](https://travis-ci.org/tipsy/javalin/builds)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven](https://img.shields.io/maven-central/v/io.javalin/javalin.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22io.javalin%22%20AND%20a%3A%22javalin%22)

# Javalin - A Simple REST API Library for Java/Kotlin

The project webpage is [javalin.io](https://javalin.io).

## Java quickstart

### Add dependency (maven)

```xml
<dependency>
    <groupId>io.javalin</groupId>
    <artifactId>javalin</artifactId>
    <version>1.2.0</version>
</dependency>
```

### Start programming:

```java
import io.javalin.Javalin;

public class HelloWorld {
    public static void main(String[] args) {
        Javalin app = Javalin.start(7000);
        app.get("/", ctx -> ctx.result("Hello World"));
    }
}
```

## Kotlin quickstart

### Add dependency (gradle)
```kotlin
compile 'io.javalin:javalin:1.2.0'
```

### Start programming
```kotlin
import io.javalin.Javalin

fun main(args: Array<String>) {
    val app = Javalin.start(7000)
    app.get("/") { ctx -> ctx.result("Hello World") }
}
```

## Special thanks
* Blake Mizerany, for creating [Sinatra](http://www.sinatrarb.com/)
* Per Wendel, for creating [Spark](http://sparkjava.com/)
* [Christian Rasmussen](https://github.com/chrrasmussen), for being a great guy
* [Per Kristian Kummermo](https://github.com/pkkummermo), also for being a great guy
