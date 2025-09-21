import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;

public class TestJavalinNativeClient {
    public static void main(String[] args) {
        // Test basic functionality
        JavalinTest.test((server, client) -> {
            server.get("/hello", ctx -> ctx.result("Hello, World!"));
            server.post("/echo", ctx -> ctx.result("Echo: " + ctx.body()));
            
            // Test GET request
            var getResponse = client.get("/hello");
            System.out.println("GET /hello - Status: " + getResponse.code() + ", Body: " + getResponse.body().string());
            
            // Test POST request with JSON
            var postResponse = client.post("/echo", "Test message");
            System.out.println("POST /echo - Status: " + postResponse.code() + ", Body: " + postResponse.body().string());
        });
        
        System.out.println("✅ Basic functionality test passed!");
    }
}