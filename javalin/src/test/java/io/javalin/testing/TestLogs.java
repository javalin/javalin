package io.javalin.testing;

import io.javalin.component.Component;

public class TestLogs implements Component {
    public String value;
    public TestLogs(String value) {
        this.value = value;
    }
}
