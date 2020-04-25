package io.javalin.examples;

import io.javalin.plugin.graphql.graphql.QueryGraphql;

public class QueryExampleJVM implements QueryGraphql {
    public String hello() {
        return "Hello world";
    }
}
