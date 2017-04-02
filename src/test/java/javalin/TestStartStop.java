package javalin;

import org.junit.Test;


public class TestStartStop {

    @Test
    public void test_waitsWorks_whenCalledInCorrectOrder() throws Exception {
        Javalin.create().start().awaitInitialization().stop().awaitTermination();
    }

    @Test(expected = IllegalStateException.class)
    public void test_awaitInitThrowsException_whenNotStarted() throws Exception {
        Javalin.create().awaitInitialization();
    }

    @Test(expected = IllegalStateException.class)
    public void test_awaitTerminationThrowsException_whenNotStopped() throws Exception {
        Javalin.create().awaitTermination();
    }

}
