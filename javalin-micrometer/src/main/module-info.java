module io.javalin.micrometer {
    exports io.javalin.micrometer;

    requires transitive io.javalin;
    requires transitive micrometer.core;
    requires kotlin.stdlib;
    requires org.slf4j;

    // Jetty 12 support
    requires static micrometer.jetty12;
    
    // Optional dependencies for specific registries
    requires static micrometer.registry.prometheus;
}
