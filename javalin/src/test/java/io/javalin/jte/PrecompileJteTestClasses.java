package io.javalin.jte;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.DirectoryCodeResolver;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PrecompileJteTestClasses {

    public static final String PACKAGE_NAME = "io.javalin.jte.precompiled";

    public static void main(String[] args) {
        Path inputDirectory = Paths.get("javalin/src/test/resources/templates/jte");
        Path outputDirectory = Paths.get("javalin/src/test/java");

        TemplateEngine templateEngine = TemplateEngine.create(new DirectoryCodeResolver(inputDirectory), outputDirectory, ContentType.Html, null, PACKAGE_NAME);
        templateEngine.generateAll();
    }
}
