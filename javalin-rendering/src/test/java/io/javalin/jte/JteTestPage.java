package io.javalin.jte;

public class JteTestPage {
    private final String hello;
    private final String world;

    public JteTestPage(String hello, String world) {
        this.hello = hello;
        this.world = world;
    }

    public String getHello() {
        return hello;
    }

    public String getWorld() {
        return world;
    }
}
