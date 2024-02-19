/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.rendering.util

import io.javalin.util.DependencyUtil
import io.javalin.util.OptionalDependency;
import io.javalin.util.Util

enum class RenderingDependency(
    override val displayName: String,
    override val testClass: String,
    override val groupId: String,
    override val artifactId: String,
    override val version: String
) : OptionalDependency {
    JTE("jte", "gg.jte.TemplateEngine", "gg.jte", "jte", "2.2.1"),
    JTE_KOTLIN("jte-kotlin", "gg.jte.compiler.kotlin.KotlinClassCompiler", "gg.jte", "jte-kotlin", "2.2.1"),
    VELOCITY("Velocity", "org.apache.velocity.app.VelocityEngine", "org.apache.velocity", "velocity-engine-core", "2.3"),
    FREEMARKER("Freemarker", "freemarker.template.Configuration", "org.freemarker", "freemarker", "2.3.30"),
    THYMELEAF("Thymeleaf", "org.thymeleaf.TemplateEngine", "org.thymeleaf", "thymeleaf", "3.0.12.RELEASE"),
    MUSTACHE("Mustache", "com.github.mustachejava.MustacheFactory", "com.github.spullara.mustache.java", "compiler", "0.9.7"),
    PEBBLE("Pebble", "com.mitchellbosecke.pebble.PebbleEngine", "io.pebbletemplates", "pebble", "3.1.5"),
    COMMONMARK("Commonmark", "org.commonmark.renderer.html.HtmlRenderer", "org.commonmark", "commonmark", "0.17.1"),
    ;

    internal fun exists() = Util.classExists(this.testClass)
}

object Util {
    fun throwIfNotAvailable(dependency: RenderingDependency) {
        if (!Util.classExists(dependency.testClass)) {
            throw IllegalStateException(DependencyUtil.missingDependencyMessage(dependency))
        }
    }
}
