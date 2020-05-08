package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.plugin.graphql.GraphQLOptions;
import io.javalin.plugin.graphql.GraphQLPlugin;


public class HelloWorldGraphQL {
    public static void main(String[] args) {
        GraphQLOptions graphQLOptions = new GraphQLOptions("/graphql", null)
            .addPackage("io.javalin.examples")
            .register(new QueryExampleJVM());

        Javalin
            .create(config -> config.registerPlugin(new GraphQLPlugin(graphQLOptions)))
            .start(7070);

    }
}
