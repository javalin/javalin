module io.javalin.mdns {
    exports io.javalin.mdns;

    requires transitive io.javalin;
    requires transitive javax.jmdns;
    requires kotlin.stdlib;
    requires org.slf4j;

    // Used to read the bound port after the server starts
    requires static org.eclipse.jetty.server;
}
