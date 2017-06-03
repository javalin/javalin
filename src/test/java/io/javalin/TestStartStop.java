/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import org.junit.Test;


public class TestStartStop {

    @Test
    public void test_waitsWorks_whenCalledInCorrectOrder() throws Exception {
        Javalin.Companion.create().start().awaitInitialization().stop().awaitTermination();
    }

    @Test(expected = IllegalStateException.class)
    public void test_awaitInitThrowsException_whenNotStarted() throws Exception {
        Javalin.Companion.create().awaitInitialization();
    }

    @Test(expected = IllegalStateException.class)
    public void test_awaitTerminationThrowsException_whenNotStopped() throws Exception {
        Javalin.Companion.create().awaitTermination();
    }

}
