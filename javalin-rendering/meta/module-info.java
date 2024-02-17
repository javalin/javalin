module io.javalin.rendering {
	requires io.javalin;
    requires transitive kotlin.stdlib;

    requires static thymeleaf;
    requires static com.github.mustachejava;
    requires static freemarker;
    requires static gg.jte;
    requires static gg.jte.runtime;
    requires static io.pebbletemplates;

	exports io.javalin.rendering.template;
	exports io.javalin.rendering.markdown;
}
