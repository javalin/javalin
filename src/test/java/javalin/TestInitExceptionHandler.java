package javalin;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static javalin.Javalin.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

public class TestInitExceptionHandler {

    private static int NON_VALID_PORT = Integer.MAX_VALUE;
    private static Javalin app;
    private static String errorMessage = "Override me!";

    @BeforeClass
    public static void setup() throws Exception {
        app = create()
            .port(NON_VALID_PORT)
            .startupExceptionHandler((e) -> errorMessage = "Woops...")
            .start()
            .awaitInitialization();
    }

    @Test
    public void testInitExceptionHandler() throws Exception {
        assertThat(errorMessage, is("Woops..."));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        app.stop();
    }

}
