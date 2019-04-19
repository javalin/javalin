/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core;

import io.javalin.Javalin;

/**
 * An extension is a modular way of adding functionality to a Javalin instance.
 * For use with {@link Javalin#register}.
 */
@FunctionalInterface
public interface Extension {
    void registerOnJavalin(Javalin app);
}
