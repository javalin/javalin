package javalin.examples;

import javalin.Javalin;

import static javalin.ApiBuilder.*;

public class HelloWorldApi {

    public static void main(String[] args) {
        Javalin.create()
            .port(7070)
            .routes(() -> {
                get("/hello", (req, res) -> res.body("Hello World"));
                path("/api", () -> {
                    get("/test", (req, res) -> res.body("Hello World"));
                    get("/tast", (req, res) -> res.status(200).body("Hello world"));
                    get("/hest", (req, res) -> res.status(200).body("Hello World"));
                    get("/hast", (req, res) -> res.status(200).body("Hello World").header("test", "tast"));
                });
            });
    }

}
