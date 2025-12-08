# Handlebars Template Rendering Example

This document provides a complete, working example of how to use Handlebars templates with Javalin.

## Overview

While Javalin doesn't provide an official Handlebars rendering module, you can easily create your own by implementing the `FileRenderer` interface. This example shows exactly how to do that.

## Implementation

### Step 1: Add Handlebars Dependency

Add the Handlebars Java library to your project:

**Maven:**
```xml
<dependency>
    <groupId>com.github.jknack</groupId>
    <artifactId>handlebars</artifactId>
    <version>4.3.1</version>
</dependency>
```

**Gradle:**
```kotlin
implementation("com.github.jknack:handlebars:4.3.1")
```

### Step 2: Create JavalinHandlebars Renderer

Create a class that implements `FileRenderer`:

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

/**
 * FileRenderer implementation for Handlebars template engine.
 */
public class JavalinHandlebars implements FileRenderer {
    
    private final Handlebars handlebars;

    /**
     * Creates a new JavalinHandlebars with default configuration.
     * Templates are loaded from classpath "/templates" with ".hbs" extension.
     */
    public JavalinHandlebars() {
        this(defaultHandlebars());
    }

    /**
     * Creates a new JavalinHandlebars with custom Handlebars instance.
     * 
     * @param handlebars the Handlebars instance to use
     */
    public JavalinHandlebars(Handlebars handlebars) {
        this.handlebars = handlebars;
    }

    @Override
    public String render(String filePath, Map<String, ?> model, Context context) {
        try {
            // Remove file extension if present, as Handlebars will add it
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
        if (lastDot > 0) {
            return filePath.substring(0, lastDot);
        }
        return filePath;
    }
}
```

### Step 3: Configure Javalin

In Javalin 7+, configure the renderer during app creation:

```java
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinHandlebars;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        Javalin.create(config -> {
            // Configure Handlebars as the file renderer
            config.fileRenderer(new JavalinHandlebars());
            
            // Define your routes
            config.routes.get("/", ctx -> {
                Map<String, Object> model = Map.of(
                    "title", "Welcome",
                    "message", "Hello from Handlebars!",
                    "users", java.util.List.of("Alice", "Bob", "Charlie")
                );
                // This will look for templates/index.hbs
                ctx.render("index", model);
            });
        }).start(7070);
    }
}
```

**For Javalin 6:**
```java
Javalin app = Javalin.create(config -> {
    config.fileRenderer(new JavalinHandlebars());
});

app.get("/", ctx -> {
    ctx.render("index", Map.of("message", "Hello!"));
});

app.start(7070);
```

### Step 4: Create Templates

Create your Handlebars templates in `src/main/resources/templates/`:

**src/main/resources/templates/index.hbs:**
```handlebars
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>{{title}}</title>
</head>
<body>
    <h1>{{title}}</h1>
    <p>{{message}}</p>
    
    {{#if users}}
    <h2>Users:</h2>
    <ul>
        {{#each users}}
        <li>{{this}}</li>
        {{/each}}
    </ul>
    {{/if}}
</body>
</html>
```

## Advanced Configuration

### Custom Template Location

If you want to load templates from a different location:

```java
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.FileTemplateLoader;

// Load templates from filesystem
FileTemplateLoader loader = new FileTemplateLoader("my-templates", ".hbs");
Handlebars handlebars = new Handlebars(loader);

config.fileRenderer(new JavalinHandlebars(handlebars));
```

### Registering Helpers

Handlebars supports custom helpers for complex logic:

```java
Handlebars handlebars = new Handlebars(new ClassPathTemplateLoader("/templates", ".hbs"));

// Register a helper to uppercase text
handlebars.registerHelper("upper", (context, options) -> {
    return context.toString().toUpperCase();
});

// Register a helper for date formatting
handlebars.registerHelper("formatDate", (context, options) -> {
    if (context instanceof java.util.Date) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(context);
    }
    return context;
});

config.fileRenderer(new JavalinHandlebars(handlebars));
```

Use helpers in templates:
```handlebars
<p>{{upper name}}</p>
<p>Created: {{formatDate createdDate}}</p>
```

### Using Partials

Handlebars supports partials (reusable template fragments):

**templates/header.hbs:**
```handlebars
<header>
    <h1>{{siteName}}</h1>
    <nav><!-- navigation --></nav>
</header>
```

**templates/page.hbs:**
```handlebars
<!DOCTYPE html>
<html>
<head><title>{{title}}</title></head>
<body>
    {{> header}}
    <main>{{content}}</main>
</body>
</html>
```

### Template Caching

For production, enable template caching:

```java
import com.github.jknack.handlebars.cache.ConcurrentMapTemplateCache;

Handlebars handlebars = new Handlebars(new ClassPathTemplateLoader("/templates", ".hbs"));
handlebars.with(new ConcurrentMapTemplateCache());

config.fileRenderer(new JavalinHandlebars(handlebars));
```

## Complete Working Example

Here's a full example application:

```java
package com.example;

import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinHandlebars;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import java.util.Map;
import java.util.List;

public class HandlebarsApp {
    public static void main(String[] args) {
        // Configure Handlebars with custom helpers
        Handlebars handlebars = new Handlebars(new ClassPathTemplateLoader("/templates", ".hbs"));
        handlebars.registerHelper("upper", (context, options) -> 
            context.toString().toUpperCase()
        );
        
        Javalin app = Javalin.create(config -> {
            config.fileRenderer(new JavalinHandlebars(handlebars));
            
            config.routes.get("/", ctx -> {
                ctx.render("index", Map.of(
                    "title", "Home",
                    "message", "Welcome to Javalin + Handlebars",
                    "features", List.of(
                        "Simple integration",
                        "Custom helpers",
                        "Partials support",
                        "Template caching"
                    )
                ));
            });
            
            config.routes.get("/users/{name}", ctx -> {
                String name = ctx.pathParam("name");
                ctx.render("user", Map.of(
                    "title", "User Profile",
                    "username", name,
                    "isAdmin", name.equals("admin")
                ));
            });
        });
        
        app.start(7070);
        System.out.println("Server started at http://localhost:7070");
    }
}
```

## Handlebars Syntax Quick Reference

### Variables
```handlebars
{{name}}
{{user.firstName}}
```

### Conditionals
```handlebars
{{#if isActive}}
    <p>Active user</p>
{{else}}
    <p>Inactive user</p>
{{/if}}

{{#unless isEmpty}}
    <p>Has content</p>
{{/unless}}
```

### Loops
```handlebars
{{#each items}}
    <li>{{this}}</li>
{{/each}}

{{#each users}}
    <li>{{name}} - {{email}}</li>
{{/each}}
```

### Comments
```handlebars
{{! This is a comment }}
{{!-- This is a multi-line
      comment --}}
```

### Escaping
```handlebars
{{name}}       <!-- HTML escaped -->
{{{rawHtml}}}  <!-- Unescaped -->
```

## Why Not an Official Module?

You might wonder why Handlebars isn't included as an official javalin-rendering module like Mustache, Freemarker, or Thymeleaf. The reasons include:

1. **Mustache is similar** - Mustache and Handlebars have very similar syntax, and Mustache is already supported
2. **Easy to implement** - As this example shows, creating a custom renderer is straightforward
3. **Flexibility** - Custom implementations allow you to configure Handlebars exactly how you need

However, if there's enough community interest, Handlebars support could be added as an official module. Feel free to open an issue or PR on the Javalin repository!

## Troubleshooting

### Template not found

Make sure your templates are in `src/main/resources/templates/` and the path is correct:
```java
// For templates/index.hbs
ctx.render("index", model);  // Correct
ctx.render("templates/index", model);  // Also works
ctx.render("templates/index.hbs", model);  // Also works
```

### ClassNotFoundException

Make sure you've added the Handlebars dependency to your `pom.xml` or `build.gradle`.

### Template doesn't update

During development, you may need to disable caching or rebuild your project to see template changes.

## Further Reading

- [Handlebars.java GitHub](https://github.com/jknack/handlebars.java)
- [Handlebars.js Documentation](https://handlebarsjs.com/) (JavaScript version, but syntax is compatible)
- [Javalin Documentation](https://javalin.io/documentation)
- [Javalin Rendering Plugin](https://javalin.io/plugins/rendering)

## Contributing

If you'd like to contribute an official Handlebars module to Javalin:

1. Fork the Javalin repository
2. Create a new module in `javalin-utils/javalin-rendering/javalin-rendering-handlebars`
3. Follow the pattern used by other rendering modules
4. Add tests
5. Submit a pull request

The maintainers would be happy to review it!
