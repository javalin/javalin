package javalin.examples;

import javalin.Javalin;

public class HelloWorld {
    public static void main(String[] args) {
        Javalin app = Javalin.create().port(7000);
        app.get("/", (req, res) -> res.body("Hello World"));
    }
}
