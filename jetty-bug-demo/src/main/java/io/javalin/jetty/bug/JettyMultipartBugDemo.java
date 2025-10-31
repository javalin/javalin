package io.javalin.jetty.bug;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.MultiPartConfig;
import org.eclipse.jetty.http.MultiPartFormData;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * FINAL ATTEMPT: Jetty 12.1.3 multipart bug.
 * 
 * Key insight: Let CompletionException propagate to Jetty's error handling,
 * which should trigger the cleanup path where the bug occurs.
 */
public class JettyMultipartBugDemo {

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(0);
        server.addConnector(connector);

        // Use Core Handler with error page
        Handler.Sequence handlers = new Handler.Sequence();
        
        // Main handler that throws exception
        handlers.addHandler(new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) throws Exception {
                System.out.println("Handler: Parsing multipart...");
                
                String contentType = request.getHeaders().get("Content-Type");
                MultiPartConfig config = new MultiPartConfig.Builder().maxPartSize(10).build();
                
                // This throws CompletionException - let it propagate!
                MultiPartFormData.Parts parts = MultiPartFormData.getParts(
                        request, request, contentType, config);
                
                response.setStatus(HttpStatus.OK_200);
                response.write(true, StandardCharsets.UTF_8.encode("Success: " + parts.size()), callback);
                return true;
            }
        });
        
        // Error handler
        handlers.addHandler(new Handler.Wrapper() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) throws Exception {
                try {
                    return super.handle(request, response, callback);
                } catch (Exception e) {
                    System.err.println("Error handler caught: " + e.getClass().getSimpleName());
                    response.setStatus(HttpStatus.BAD_REQUEST_400);
                    response.write(true, StandardCharsets.UTF_8.encode("Error: " + e.getMessage()), callback);
                    return true;
                }
            }
        });

        server.setHandler(handlers);
        server.start();
        int port = connector.getLocalPort();
        
        System.out.println("=".repeat(70));
        System.out.println("Jetty 12.1.3 Bug - Exception Propagation");
        System.out.println("Server: localhost:" + port);
        System.out.println("=".repeat(70));

        Thread.sleep(500);
        String result = makeRequest(port);
        server.stop();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("RESULT: " + result);
        System.out.println("=".repeat(70) + "\n");

        if (result.contains("failed to respond") || result.contains("Connection reset") || 
                result.startsWith("ERROR:")) {
            System.out.println("✅ BUG REPRODUCED!");
            System.exit(1);
        } else {
            System.out.println("❌ Bug not reproduced");
            System.exit(0);
        }
    }

    private static String makeRequest(int port) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost("http://localhost:" + port + "/");
            File testFile = new File("jetty-bug-demo/test-file.bin");
            if (!testFile.exists()) return "ERROR: Test file not found";
            
            post.setEntity(MultipartEntityBuilder.create()
                    .addBinaryBody("upload", testFile)
                    .addTextBody("text-field", "text")
                    .build());
            
            System.out.println("\nClient: Uploading " + testFile.length() + " bytes...");
            HttpResponse response = httpClient.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            return "HTTP " + statusCode + ": " + body;
        } catch (Exception e) {
            return "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }
}
