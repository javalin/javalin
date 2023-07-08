package io.javalin.micrometer;

import org.junit.jupiter.api.Test;

public class MicrometerPluginTest_Java {

    @Test
    public void api_looks_ok_from_java() {
        var plugin = new MicrometerPlugin(config -> {});
        var handler = MicrometerPlugin.exceptionHandler;
    }

}
