package io.javalin.testtools;

import io.javalin.Javalin;

@FunctionalInterface
public interface TestCase {
    void accept(Javalin server, HttpClient client) throws Exception;
}
