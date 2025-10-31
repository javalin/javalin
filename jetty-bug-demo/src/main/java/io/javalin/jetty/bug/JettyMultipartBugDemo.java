package io.javalin.jetty.bug;

import jakarta.servlet.MultipartConfigElement;
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
 * Minimal reproducible example of Jetty 12.1.1 multipart bug.
 * 
 * This demonstrates that when multipart parsing fails due to size limits,
 * Jetty 12.1.1 closes the connection without sending an error response.
 * 
 * Expected behavior (works in 12.1.0):
 * - Server catches the parsing exception
 * - Server sends HTTP 400 error response
 * - Client receives the error response
 * 
 * Actual behavior (broken in 12.1.1+):
 * - Server catches the parsing exception
 * - Server tries to send error response
 * - During cleanup, getParts() throws uncaught CompletionException
 * - Connection closes prematurely
 * - Client sees "Connection reset" or no response
 * 
 * Bug details:
 * - HttpChannelState.java:769 calls MultiPartFormData.getParts() during cleanup
 * - MultiPartFormData.java:133 calls futureParts.join() on failed CompletableFuture
 * - join() throws uncaught CompletionException
 * - Connection closes before error response is sent
 */
public class JettyMultipartBugDemo {

    public static void main(String[] args) throws Exception {
        // Create Jetty server with servlet
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(0); // Use ephemeral port
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Add servlet that handles multipart with size limit
        ServletHolder holder = new ServletHolder(new HttpServlet() {
            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                try {
                    // Set multipart config with tiny 10-byte limit
                    req.setAttribute("org.eclipse.jetty.multipartConfig",
                            new MultipartConfigElement("/tmp", 10, 10, 5));

                    // Try to get parts - this will fail when size exceeds 10 bytes
                    var parts = req.getParts();

                    // If we get here, parsing succeeded
                    resp.setStatus(200);
                    resp.setContentType("text/plain");
                    PrintWriter writer = resp.getWriter();
                    writer.write("Success: " + parts.size() + " parts");
                    writer.flush();

                } catch (Exception e) {
                    // We expect to catch the size limit exception here
                    System.err.println("Caught exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());

                    // Try to send error response
                    resp.setStatus(400);
                    resp.setContentType("text/plain");
                    PrintWriter writer = resp.getWriter();
                    writer.write("Error: " + e.getMessage());
                    writer.flush();
                }
            }
        });
        context.addServlet(holder, "/upload");

        // Start server
        server.start();
        int port = connector.getLocalPort();
        System.out.println("Server started on port " + port);

        // Give server time to fully start
        Thread.sleep(500);

        // Make client request
        CompletableFuture<String> resultFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return makeMultipartRequest(port);
            } catch (Exception e) {
                return "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        });

        // Wait for result
        String result;
        try {
            result = resultFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            result = "TIMEOUT: Request did not complete in time";
        }

        // Stop server
        server.stop();

        // Print results
        System.out.println("\n========================================");
        System.out.println("RESULT:");
        System.out.println("========================================");
        System.out.println(result);
        System.out.println("========================================\n");

        // Check if bug occurred
        if (result.contains("Connection reset") || result.contains("SocketException") ||
                result.contains("ERROR") || result.contains("Broken pipe")) {
            System.err.println("❌ BUG REPRODUCED!");
            System.err.println("Connection was closed without sending error response.");
            System.err.println("\nThis is the Jetty 12.1.1 multipart bug:");
            System.err.println("1. Multipart parsing fails (size > 10 bytes)");
            System.err.println("2. CompletableFuture<Parts> completes exceptionally");
            System.err.println("3. During cleanup, HttpChannelState.java:769 calls getParts()");
            System.err.println("4. getParts() at line 133 calls join() on failed future");
            System.err.println("5. join() throws uncaught CompletionException");
            System.err.println("6. Connection closes before error response is sent");
            System.exit(1);
        } else if (result.contains("HTTP 400") || result.contains("Error:")) {
            System.out.println("✅ NO BUG - Error response was successfully sent!");
            System.out.println("Server handled the exception and sent an error response.");
            System.exit(0);
        } else {
            System.err.println("⚠️  UNEXPECTED RESULT");
            System.exit(2);
        }
    }

    private static String makeMultipartRequest(int port) throws IOException {
        String boundary = "----WebKitFormBoundary";
        String contentType = "multipart/form-data; boundary=" + boundary;

        // Multipart content > 10 bytes
        String content = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                "This content is definitely more than 10 bytes!\r\n" +
                "--" + boundary + "--\r\n";

        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        HttpURLConnection conn = (HttpURLConnection) URI.create("http://localhost:" + port + "/upload")
                .toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", contentType);
        conn.setRequestProperty("Content-Length", String.valueOf(contentBytes.length));
        conn.setDoOutput(true);
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);

        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            os.write(contentBytes);
            os.flush();
        }

        // Try to read response
        try {
            int responseCode = conn.getResponseCode();
            String responseBody;

            if (responseCode >= 400) {
                responseBody = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            } else {
                responseBody = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }

            return "HTTP " + responseCode + ": " + responseBody;

        } catch (IOException e) {
            // Connection reset happens here
            return "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }
}
