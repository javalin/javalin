import io.javalin.Javalin;

public class JavalinTest {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            // Basic configuration test
        }).get("/", ctx -> ctx.result("Hello Javalin with Kotlin 2.0.21!"))
          .get("/health", ctx -> ctx.json(java.util.Map.of("status", "ok", "kotlin", "2.0.21")))
          .start(7070);
        
        System.out.println("✅ Javalin started successfully with Kotlin 2.0.21!");
        System.out.println("Test server at: http://localhost:7070/");
        System.out.println("Health endpoint: http://localhost:7070/health");
        
        app.stop();
        System.out.println("✅ Javalin stopped successfully!");
    }
}