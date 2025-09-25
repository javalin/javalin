module io.javalin.micrometer {
    exports io.javalin.micrometer;

    requires transitive io.javalin;
    requires transitive micrometer.core;
    requires kotlin.stdlib;
    requires org.slf4j;

    // Optional dependencies for specific registries
    requires static micrometer.registry.prometheus;
}