/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.testing;

public class TypedException extends Exception {
    public String proofOfType() {
        return "I'm so typed";
    }
}
