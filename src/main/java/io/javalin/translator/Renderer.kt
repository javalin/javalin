/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator

import io.javalin.Context
import io.javalin.core.util.Util
import io.javalin.translator.markdown.JavalinCommonmarkPlugin
import io.javalin.translator.template.*

class Renderer(val ctx: Context) {

    /**
     * Renders a Velocity template with specified values as ctx.html and
     * sets it as the context result. Sets content-type to text/ctx.html.
     * Requires Apache Velocity library in the classpath.
     */
    @JvmOverloads
    fun velocity(templatePath: String, model: Map<String, Any?> = emptyMap()): Context {
        Util.ensureDependencyPresent("Apache Velocity", "org.apache.velocity.Template", "org.apache.velocity/velocity")
        return ctx.html(JavalinVelocityPlugin.render(templatePath, model))
    }

    /**
     * Renders a Freemarker template with specified values as ctx.html and
     * sets it as the context result. Sets content-type to text/ctx.html.
     * Requires Freemarker library in the classpath.
     */
    @JvmOverloads
    fun freemarker(templatePath: String, model: Map<String, Any?> = emptyMap()): Context {
        Util.ensureDependencyPresent("Apache Freemarker", "freemarker.template.Configuration", "org.freemarker/freemarker")
        return ctx.html(JavalinFreemarkerPlugin.render(templatePath, model))
    }

    /**
     * Renders a Thymeleaf template with specified values as ctx.html and
     * sets it as the context result. Sets content-type to text/ctx.html.
     * Requires Thymeleaf library in the classpath.
     */
    @JvmOverloads
    fun thymeleaf(templatePath: String, model: Map<String, Any?> = emptyMap()): Context {
        Util.ensureDependencyPresent("Thymeleaf", "org.thymeleaf.TemplateEngine", "org.thymeleaf/thymeleaf-spring3")
        return ctx.html(JavalinThymeleafPlugin.render(templatePath, model))
    }

    /**
     * Renders a Mustache template with specified values as ctx.html and
     * sets it as the context result. Sets content-type to text/ctx.html.
     * Requires Mustache library in the classpath.
     */
    @JvmOverloads
    fun mustache(templatePath: String, model: Map<String, Any?> = emptyMap()): Context {
        Util.ensureDependencyPresent("Mustache", "com.github.mustachejava.Mustache", "com.github.spullara.mustache.java/compiler")
        return ctx.html(JavalinMustachePlugin.render(templatePath, model))
    }

    /**
     * Renders a jTwig template with specified values as ctx.html and
     * sets it as the context result. Sets content-type to text/ctx.html.
     * Requires jTwig library in the classpath.
     */
    @JvmOverloads
    fun jtwig(templatePath: String, model: Map<String, Any?> = emptyMap()): Context {
        Util.ensureDependencyPresent("jTwig", "org.jtwig.JtwigTemplate", "org.jtwig/jtwig-core")
        return ctx.html(JavalinJtwigPlugin.render(templatePath, model))
    }

    /**
     * Renders a Pebble template with specified values as ctx.html and
     * sets it as the context result. Sets content-type to text/ctx.html.
     * Requires Pebble library in the classpath.
     */
    @JvmOverloads
    fun pebble(templatePath: String, model: Map<String, Any?> = emptyMap()): Context {
        Util.ensureDependencyPresent("pebble", "com.mitchellbosecke.pebble.PebbleEngine", "com.mitchellbosecke/pebble")
        return ctx.html(JavalinPebblePlugin.render(templatePath, model))
    }

    /**
     * Renders a markdown-file and sets it as the context result.
     * Sets content-type to text/ctx.html.
     * Requires Commonmark library in the classpath.
     */
    fun markdown(markdownFilePath: String): Context {
        Util.ensureDependencyPresent("Commonmark", "org.commonmark.renderer.html.HtmlRenderer", "com.atlassian.commonmark/commonmark")
        return ctx.html(JavalinCommonmarkPlugin.render(markdownFilePath))
    }

}


