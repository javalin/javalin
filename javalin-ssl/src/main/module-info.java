module io.javalin.community.ssl {
  exports io.javalin.community.ssl;
  exports io.javalin.community.ssl.util;

  requires transitive io.javalin;
  requires org.eclipse.jetty.server;
  requires org.eclipse.jetty.alpn.server;
  requires org.eclipse.jetty.http2.server;
  requires nl.altindag.ssl;
  requires nl.altindag.ssl.jetty;
  requires nl.altindag.ssl.pem;

  requires kotlin.stdlib;
}
