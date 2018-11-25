/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

/**
 * An extension is a feature module adding a group of functionality to the Javalin application.
 *
 * It's similar to Handler groups but allows to read and modify the Javalin app instance.
 * Helpful where you like to group a cross-cutting concern in a class or lambda expression.
 *
 * Inspired by Sinatra extensions and the `Sinatra.register` DSL.
 *
 * @link http://sinatrarb.com/extensions.html#extending-the-dsl-class-context-with-sinatraregister
 */
@FunctionalInterface
public interface Extension {

  void addToJavalin(Javalin app);
}
