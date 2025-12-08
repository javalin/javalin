# Javalin Rendering

This module provides template rendering support for Javalin through the `FileRenderer` interface.

## Overview

The javalin-rendering plugin (previously a separate repository, now part of the monorepo) provides easy integration with various template engines. Each template engine is available as a separate module that you can add to your project.

## Architecture

All template engine implementations follow a simple pattern:

1. Implement the `FileRenderer` interface from `io.javalin.rendering.FileRenderer`
2. Configure the renderer in your Javalin app using `config.fileRenderer()`
3. Use `ctx.render()` in your handlers to render templates

```java
// The FileRenderer interface
fun interface FileRenderer {
    fun render(filePath: String, model: Map<String, Any?>, context: Context): String
}
```

## Available Template Engines

The following template engines are officially supported:

| Template Engine | Module | Maven Artifact |
|-----------------|--------|----------------|
| Velocity | javalin-rendering-velocity | `io.javalin:javalin-rendering-velocity` |
| Freemarker | javalin-rendering-freemarker | `io.javalin:javalin-rendering-freemarker` |
| Thymeleaf | javalin-rendering-thymeleaf | `io.javalin:javalin-rendering-thymeleaf` |
| Mustache | javalin-rendering-mustache | `io.javalin:javalin-rendering-mustache` |
| Pebble | javalin-rendering-pebble | `io.javalin:javalin-rendering-pebble` |
| CommonMark | javalin-rendering-commonmark | `io.javalin:javalin-rendering-commonmark` |
| JTE | javalin-rendering-jte | `io.javalin:javalin-rendering-jte` |

**Note:** Handlebars is not currently supported as an official module, but you can easily create your own implementation (see Custom Renderers section below).

## Usage

### 1. Add Dependency

Choose the template engine you want to use and add it to your project:

#### Maven
```xml
<dependency>
    <groupId>io.javalin</groupId>
    <artifactId>javalin-rendering-mustache</artifactId>
    <version>7.0.0</version>
</dependency>
```

#### Gradle
```kotlin
implementation("io.javalin:javalin-rendering-mustache:7.0.0")
```

### 2. Configure the Renderer

In Javalin 7+, configure the renderer during app creation:

```java
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinMustache;

public class App {
    public static void main(String[] args) {
        Javalin.create(config -> {
            // Configure the file renderer
            config.fileRenderer(new JavalinMustache());
            
            // Define your routes
            config.routes.get("/", ctx -> {
                Map<String, Object> model = Map.of("message", "Hello World!");
                ctx.render("templates/index.mustache", model);
            });
        }).start(7070);
    }
}
```

### 3. Create Templates

Place your templates in `src/main/resources` (or wherever your template engine is configured to look):

**templates/index.mustache:**
```mustache
<!DOCTYPE html>
<html>
<head>
    <title>My Page</title>
</head>
<body>
    <h1>{{message}}</h1>
</body>
</html>
```

## Examples for Each Template Engine

### Mustache
```java
import io.javalin.rendering.template.JavalinMustache;

Javalin.create(config -> {
    config.fileRenderer(new JavalinMustache());
    config.routes.get("/", ctx -> 
        ctx.render("templates/page.mustache", Map.of("name", "World"))
    );
}).start(7070);
```

### Freemarker
```java
import io.javalin.rendering.template.JavalinFreemarker;

Javalin.create(config -> {
    config.fileRenderer(new JavalinFreemarker());
    config.routes.get("/", ctx -> 
        ctx.render("templates/page.ftl", Map.of("name", "World"))
    );
}).start(7070);
```

### Thymeleaf
```java
import io.javalin.rendering.template.JavalinThymeleaf;

Javalin.create(config -> {
    config.fileRenderer(new JavalinThymeleaf());
    config.routes.get("/", ctx -> 
        ctx.render("templates/page.html", Map.of("name", "World"))
    );
}).start(7070);
```

### Velocity
```java
import io.javalin.rendering.template.JavalinVelocity;

Javalin.create(config -> {
    config.fileRenderer(new JavalinVelocity());
    config.routes.get("/", ctx -> 
        ctx.render("templates/page.vm", Map.of("name", "World"))
    );
}).start(7070);
```

### Pebble
```java
import io.javalin.rendering.template.JavalinPebble;

Javalin.create(config -> {
    config.fileRenderer(new JavalinPebble());
    config.routes.get("/", ctx -> 
        ctx.render("templates/page.peb", Map.of("name", "World"))
    );
}).start(7070);
```

### JTE
```java
import io.javalin.rendering.template.JavalinJte;

Javalin.create(config -> {
    config.fileRenderer(JavalinJte.create());
    config.routes.get("/", ctx -> 
        ctx.render("templates/page.jte", Map.of("name", "World"))
    );
}).start(7070);
```

## Custom Renderers

You can create your own renderer for any template engine by implementing the `FileRenderer` interface. This is useful for:
- Using template engines not officially supported (like Handlebars)
- Customizing the configuration of existing template engines
- Creating specialized rendering logic

### Example: Custom Handlebars Renderer

Here's how to create a Handlebars renderer:

#### 1. Add Handlebars dependency

```xml
<dependency>
    <groupId>com.github.jknack</groupId>
    <artifactId>handlebars</artifactId>
    <version>4.3.1</version>
</dependency>
```

#### 2. Create the renderer class

```java
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import io.javalin.http.Context;
import io.javalin.rendering.FileRenderer;
import java.io.IOException;

public class JavalinHandlebars implements FileRenderer {
    private final Handlebars handlebars;

    public JavalinHandlebars() {
        this(defaultHandlebars());
    }

    public JavalinHandlebars(Handlebars handlebars) {
        this.handlebars = handlebars;
    }

    @Override
    public String render(String filePath, Map<String, Object> model, Context context) {
        try {
            // Remove file extension if present (Handlebars adds it)
            String templateName = filePath.replaceFirst("\\.[^.]+$", "");
            return handlebars.compile(templateName).apply(model);
        } catch (IOException e) {
            throw new RuntimeException("Failed to render template: " + filePath, e);
        }
    }

    private static Handlebars defaultHandlebars() {
        TemplateLoader loader = new ClassPathTemplateLoader("/templates", ".hbs");
        return new Handlebars(loader);
    }
}
```

#### 3. Use it in your app

```java
import io.javalin.Javalin;

public class App {
    public static void main(String[] args) {
        Javalin.create(config -> {
            config.fileRenderer(new JavalinHandlebars());
            
            config.routes.get("/", ctx -> {
                Map<String, Object> model = Map.of(
                    "title", "Welcome",
                    "message", "Hello from Handlebars!"
                );
                ctx.render("templates/index.hbs", model);
            });
        }).start(7070);
    }
}
```

#### 4. Create your template

**src/main/resources/templates/index.hbs:**
```handlebars
<!DOCTYPE html>
<html>
<head>
    <title>{{title}}</title>
</head>
<body>
    <h1>{{message}}</h1>
</body>
</html>
```

### Custom Configuration

You can also customize the configuration of existing renderers:

```java
// Custom Mustache configuration
MustacheFactory factory = new DefaultMustacheFactory("custom/path");
config.fileRenderer(new JavalinMustache(factory));

// Custom Freemarker configuration
Configuration fmConfig = new Configuration(Configuration.VERSION_2_3_32);
fmConfig.setDirectoryForTemplateLoading(new File("templates"));
config.fileRenderer(new JavalinFreemarker(fmConfig));
```

## Migration from Archived Repository

If you were using the old `javalin-rendering` plugin from the archived repository:

1. **The plugin is now part of the main Javalin monorepo** - no separate repository needed
2. **Module names have changed** - use the new artifact names (e.g., `javalin-rendering-mustache`)
3. **Configuration is the same** - use `config.fileRenderer()` in Javalin 7+
4. **All features are preserved** - the API remains compatible

## Frequently Asked Questions

### Q: Where is the javalin-rendering repository?
A: The javalin-rendering plugin was moved from a separate repository into the main Javalin monorepo under `javalin-utils/javalin-rendering`. The separate repository was archived, but the functionality is fully maintained here.

### Q: Does Javalin support Handlebars?
A: Handlebars is not currently available as an official module, but you can easily create your own renderer by implementing the `FileRenderer` interface (see the Custom Renderers section above).

### Q: How do I switch template engines?
A: Just change which renderer you configure:
```java
// Instead of Mustache:
config.fileRenderer(new JavalinMustache());

// Use Thymeleaf:
config.fileRenderer(new JavalinThymeleaf());
```

### Q: Can I use multiple template engines?
A: You can only configure one `FileRenderer` at a time. However, you can create a custom renderer that delegates to different engines based on file extension:

```java
public class MultiTemplateRenderer implements FileRenderer {
    private final Map<String, FileRenderer> renderers = Map.of(
        ".mustache", new JavalinMustache(),
        ".ftl", new JavalinFreemarker(),
        ".html", new JavalinThymeleaf()
    );
    
    @Override
    public String render(String filePath, Map<String, Object> model, Context ctx) {
        String extension = filePath.substring(filePath.lastIndexOf("."));
        FileRenderer renderer = renderers.get(extension);
        if (renderer == null) {
            throw new IllegalArgumentException("No renderer for: " + extension);
        }
        return renderer.render(filePath, model, ctx);
    }
}
```

### Q: Where should I put my templates?
A: By default, most renderers look in `src/main/resources/`. The exact location depends on the template engine configuration. Check the specific renderer's documentation or source code for details.

### Q: How do I pass data to templates?
A: Use a `Map<String, Object>` as the model:
```java
ctx.render("template.html", Map.of(
    "user", currentUser,
    "items", itemList,
    "count", 42
));
```

## Contributing

If you want to add support for a new template engine:

1. Create a new module under `javalin-utils/javalin-rendering/javalin-rendering-{engine}`
2. Implement the `FileRenderer` interface
3. Add tests following the pattern of existing modules
4. Update this README
5. Submit a pull request

## License

Apache License 2.0
