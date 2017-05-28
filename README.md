[![Chat at https://gitter.im/javalin-io/general](https://badges.gitter.im/javalin-io/general.svg)](https://gitter.im/javalin-io/general)
![](https://img.shields.io/travis/tipsy/javalin.svg) 
![](https://img.shields.io/github/license/tipsy/javalin.svg)
![](https://img.shields.io/maven-central/v/io.javalin/javalin.svg)

# Javalin - A Simple REST API Library for Java/Kotlin

The project webpage is [javalin.io](https://javalin.io).

## Getting started

### Add the dependency

```xml
<dependency>
    <groupId>io.javalin</groupId>
    <artifactId>javalin</artifactId>
    <version>0.1.1</version>
</dependency>
```

### Start programming:

```java
import javalin.Javalin;

public class HelloWorld {
    public static void main(String[] args) {
        Javalin app = Javalin.create().port(7000);
        app.get("/", (req, res) -> res.body("Hello World"));
    }
}
```
