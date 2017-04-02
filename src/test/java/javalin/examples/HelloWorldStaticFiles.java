package javalin.examples;

import javalin.Javalin;

public class HelloWorldStaticFiles {

    public static void main(String[] args) {
        Javalin.create()
            .port(7070)
            .enableStaticFiles("/public");
    }

}
