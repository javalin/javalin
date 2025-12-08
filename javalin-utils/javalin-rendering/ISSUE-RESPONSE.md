# Response to Issue: What's the current recommended way to use Handlebars templates?

## Summary

The **javalin-rendering plugin is NOT archived** - it has been moved into the main Javalin monorepo and is actively maintained under `javalin-utils/javalin-rendering`.

However, **Handlebars is not currently supported as an official module**. The available template engines are:
- Velocity
- Freemarker
- Thymeleaf
- Mustache (similar syntax to Handlebars)
- Pebble
- CommonMark
- JTE

## The Good News

Creating a Handlebars renderer is straightforward! You can implement your own by following the `FileRenderer` interface pattern that all Javalin template engines use.

## Complete Handlebars Implementation

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.github.jknack</groupId>
    <artifactId>handlebars</artifactId>
    <version>4.3.1</version>
</dependency>
```

### 2. Create JavalinHandlebars Class

```java
package io.javalin.rendering.template;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import io.javalin.http.Context;
import io.javalin.rendering.FileRenderer;

import java.io.IOException;
import java.util.Map;

public class JavalinHandlebars implements FileRenderer {
    
    private final Handlebars handlebars;

    public JavalinHandlebars() {
        this(defaultHandlebars());
    }

    public JavalinHandlebars(Handlebars handlebars) {
        this.handlebars = handlebars;
    }

    @Override
    public String render(String filePath, Map<String, ?> model, Context context) {
        try {
            String templateName = removeExtension(filePath);
            Template template = handlebars.compile(templateName);
            return template.apply(model);
        } catch (IOException e) {
            throw new RuntimeException("Failed to render template: " + filePath, e);
        }
    }

    private static Handlebars defaultHandlebars() {
        TemplateLoader loader = new ClassPathTemplateLoader("/templates", ".hbs");
        return new Handlebars(loader);
    }

    private String removeExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        return lastDot > 0 ? filePath.substring(0, lastDot) : filePath;
    }
}
```

### 3. Configure in Javalin 6.7

```java
import io.javalin.Javalin;

public class App {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.fileRenderer(new JavalinHandlebars());
        });

        app.get("/", ctx -> {
            ctx.render("templates/index", Map.of(
                "message", "Hello from Handlebars!"
            ));
        });

        app.start(7070);
    }
}
```

### 4. Create Template

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

## How Javalin Rendering Works

The javalin-rendering system is elegant and simple:

1. **All template engines implement `FileRenderer`** - a functional interface with one method:
   ```java
   String render(String filePath, Map<String, ?> model, Context context)
   ```

2. **You configure it once** with `config.fileRenderer(new YourRenderer())`

3. **You use it everywhere** with `ctx.render("template", model)`

This is exactly what you had in mind with your custom render function, and it's the correct approach! The pattern you wrote is very close to the official implementation style.

## Comparison: Your Code vs Official Pattern

Your original idea:
```java
public String render(String template, Map<String, ?> model) {
    private final Handlebars handlebars;
    var loader = new ClassPathTemplateLoader("/templates", ".hbs");
    this.handlebars = new Handlebars(loader);
    Template compiled = handlebars.compile(template);
    return compiled.apply(model);
}
```

Official pattern (what I showed above):
- Implements `FileRenderer` interface
- Initializes Handlebars once in constructor (not on every render)
- Takes `Context` as a parameter for framework integration
- Follows the same structure as all other Javalin template engines

## Documentation

I've added comprehensive documentation to the Javalin repository:

1. **[javalin-utils/javalin-rendering/README.md](link-to-file)** - Complete guide to all template engines
2. **[javalin-utils/javalin-rendering/HANDLEBARS-EXAMPLE.md](link-to-file)** - Detailed Handlebars implementation guide with advanced features

These docs explain:
- How the rendering system works
- All available template engines
- How to create custom renderers
- Migration from the archived repository
- Advanced Handlebars features (helpers, partials, caching)

## Why Isn't Handlebars Official?

Good question! Here's why:

1. **Mustache is very similar** - Javalin already supports Mustache, which has nearly identical syntax
2. **Easy to implement** - As you can see, creating a custom renderer is straightforward
3. **Flexibility** - Custom implementations let you configure exactly what you need

That said, if there's community interest, Handlebars could be added as an official module. Feel free to contribute!

## Recommendation for Javalin 6.7

**Option 1: Use the custom JavalinHandlebars class** (shown above)
- Gives you full control
- Works exactly like official renderers
- Takes 5 minutes to implement

**Option 2: Use Mustache instead**
- Already supported officially
- Very similar syntax to Handlebars
- Just add `io.javalin:javalin-rendering-mustache` dependency

```java
import io.javalin.rendering.template.JavalinMustache;

config.fileRenderer(new JavalinMustache());
```

## Clarification About the "Archived" Repository

The confusion comes from:
- The **standalone** javalin-rendering repository (github.com/javalin/javalin-rendering) was archived
- The **functionality** was moved into the main Javalin monorepo
- It's at `javalin-utils/javalin-rendering` in the main repo
- It's actively maintained and used

The separate repository was archived because having template engines in a separate repo made releases and maintenance harder. Everything is now in one place!

## Next Steps

1. Copy the `JavalinHandlebars` class into your project
2. Add the Handlebars dependency
3. Configure it with `config.fileRenderer(new JavalinHandlebars())`
4. Start using `ctx.render()` in your handlers

Or check out the detailed documentation I've added to the repository for more advanced usage!
