package io.javalin.jetty.bug;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Minimal reproducible example attempting to demonstrate Jetty 12.1.1 multipart bug.
 * 
 * This mimics what Javalin does:
 * 1. Sets multipart config via request attribute
 * 2. Calls req.getParts() to trigger parsing
 * 3. Catches exception and sends error response
 * 
 * The bug should occur during cleanup when HttpChannelState.completeStream()
 * calls MultiPartFormData.getParts() which throws CompletionException.
 * 
 * However, the servlet API appears to handle cleanup differently than
 * Javalin's direct usage, so this demo may not reproduce the exact bug.
 */
public class JettyMultipartBugDemo {

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(0);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Servlet that mimics Javalin's multipart handling
        ServletHolder holder = new ServletHolder(new HttpServlet() {
            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
                    throws IOException, ServletException {
                try {
                    // Step 1: Set multipart config (like Javalin does in before handler)
                    req.setAttribute("org.eclipse.jetty.multipartConfig",
                            new MultipartConfigElement("/tmp", 10, 10, 5));

                    // Step 2: Call getParts() (like Javalin's MultipartUtil.processParts())
                    System.out.println("Calling getParts() - will fail due to size limit...");
                    var parts = req.getParts();
                    
                    // If we get here, no exception
                    System.out.println("Success: " + parts.size() + " parts");
                    resp.setStatus(200);
                    resp.setContentType("text/plain");
                    resp.getWriter().write("Success: " + parts.size() + " parts");

                } catch (Exception e) {
                    // Step 3: Exception handler (like Javalin's exception handlers)
                    System.err.println("Exception caught: " + e.getClass().getSimpleName());
                    System.err.println("  " + e.getMessage());
                    
                    // Send error response
                    resp.setStatus(400);
                    resp.setContentType("text/plain");
                    resp.getWriter().write("Error: " + e.getMessage());
                    
                    System.out.println("Error response sent");
                }
                // Method exits here - Jetty cleanup runs
                // Bug should occur in HttpChannelState.completeStream() line 769
            }
        });
        context.addServlet(holder, "/upload");

        server.start();
        int port = connector.getLocalPort();
        
        System.out.println("=".repeat(60));
        System.out.println("Jetty 12.1.1 Multipart Bug Demo");
        System.out.println("Server: http://localhost:" + port);
        System.out.println("=".repeat(60));

        Thread.sleep(500);

        // Client request
        String result = makeMultipartRequest(port);

        server.stop();

        System.out.println("\n" + "=".repeat(60));
        System.out.println("RESULT: " + result);
        System.out.println("=".repeat(60) + "\n");

        // Check result
        if (result.contains("Connection reset") || result.contains("SocketException") ||
                result.startsWith("ERROR:")) {
            System.err.println("❌ BUG REPRODUCED!");
            System.err.println("Connection closed before error response sent.");
            System.err.println("\nThis is the Jetty 12.1.1+ bug:");
            System.err.println("- HttpChannelState.java:769 calls getParts() in cleanup");
            System.err.println("- MultiPartFormData.java:133 calls join() on failed future");
            System.err.println("- CompletionException thrown, connection closes");
            System.exit(1);
        } else if (result.contains("Error") || result.contains("400")) {
            System.out.println("✅ Error response sent successfully");
            System.out.println("\nNote: Servlet API may handle cleanup differently than");
            System.out.println("Javalin's usage, which is why bug doesn't reproduce here.");
            System.out.println("The bug occurs in Javalin test with Jetty 12.1.3.");
            System.exit(0);
        } else {
            System.err.println("⚠️  Unexpected result");
            System.exit(2);
        }
    }

    private static String makeMultipartRequest(int port) {
        try {
            String boundary = "----WebKitFormBoundary";
            String content = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"upload\"; filename=\"test.txt\"\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "\r\n" +
                    "Content exceeding 10 bytes limit here!\r\n" +
                    "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"text-field\"\r\n" +
                    "\r\n" +
                    "text\r\n" +
                    "--" + boundary + "--\r\n";

            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            System.out.println("\nSending " + bytes.length + " bytes (limit is 10)...");

            HttpURLConnection conn = (HttpURLConnection) 
                    URI.create("http://localhost:" + port + "/upload").toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setDoOutput(true);
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }

            int code = conn.getResponseCode();
            System.out.println("Response code: " + code);
            
            String body = new String((code >= 400 ? conn.getErrorStream() : conn.getInputStream())
                    .readAllBytes(), StandardCharsets.UTF_8);

            return "HTTP " + code + ": " + body;

        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            return "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }
}
