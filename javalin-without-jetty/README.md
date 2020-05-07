~99% of Javalin users use Javalin with Jetty, but there is a small
subset of users who use it with Tomcat (or other web-servers).

This module is a giant hack that makes life easier for these users
without adding any complexity to the main javalin module itself,
as that would likely annoy the rest of the community

This module works as follows:

1. It pulls in the javalin module as dependency
2. It shades the javalin dependency and excludes all transitive dependencies
3. It copies relevant classes (Javalin.java, JavalinConfig.java, Context.kt)
   from the javalin-module, and performs text-replacement, changing
   access modifiers from public to private
4. It compiles these copied Java and Kotlin files, overwriting the
   class files that were originally shaded in, resulting in a
   sans-jetty friendly subset of Javalin functionality
