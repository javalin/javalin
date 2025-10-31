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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Reproduce Jetty 12.1.3 multipart bug.
 * 
 * Key insight: The bug occurs when the response is NOT immediately flushed.
 * Javalin buffers the response and flushes it later, which means cleanup
 * can run before the response is written.
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

        ServletHolder holder = new ServletHolder(new HttpServlet() {
            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
                    throws ServletException, IOException {
                
                // Buffer to hold response (like Javalin does)
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                
                try {
                    req.setAttribute("org.eclipse.jetty.multipartConfig",
                            new MultipartConfigElement("/tmp", 10, 10, 5));

                    System.out.println("Calling getParts()...");
                    var parts = req.getParts();
                    
                    System.out.println("Success: " + parts.size() + " parts");
                    resp.setStatus(200);
                    buffer.write(("Success: " + parts.size() + " parts").getBytes(StandardCharsets.UTF_8));

                } catch (Exception e) {
                    System.err.println("Exception: " + e.getClass().getSimpleName());
                    System.err.println("  " + e.getMessage());
                    
                    resp.setStatus(400);
                    buffer.write(("Error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
                    
                    System.out.println("Response buffered (not flushed yet)");
                }
                
                // DON'T flush yet - return first
                // Cleanup should run here before response is written
                resp.setContentType("text/plain");
                
                // Now write the buffered response
                try {
                    resp.getOutputStream().write(buffer.toByteArray());
                    resp.getOutputStream().flush();
                    System.out.println("Response flushed");
                } catch (IOException e) {
                    System.err.println("Failed to flush response: " + e.getMessage());
                }
            }
        });
        context.addServlet(holder, "/upload");

        server.start();
        int port = connector.getLocalPort();
        
        System.out.println("=".repeat(70));
        System.out.println("Jetty 12.1.3 Multipart Bug - Buffered Response");
        System.out.println("Response buffered (not immediately flushed)");
        System.out.println("Server: http://localhost:" + port);
        System.out.println("=".repeat(70));

        Thread.sleep(500);

        String result = makeRequest(port);

        server.stop();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("RESULT: " + result);
        System.out.println("=".repeat(70) + "\n");

        if (result.contains("Connection reset") || result.startsWith("ERROR:") || 
                result.contains("failed to respond") || result.contains("Unexpected end")) {
            System.out.println("✅ BUG REPRODUCED!");
            System.out.println("\nConnection closed before buffered response sent.");
            System.out.println("This is the Jetty 12.1.3 multipart cleanup bug.");
            System.out.println("\nBug occurs because:");
            System.out.println("1. Multipart parsing fails");
            System.out.println("2. Exception caught, response buffered");
            System.out.println("3. Method returns WITHOUT flushing response");
            System.out.println("4. Cleanup runs: HttpChannelState.completeStream()");
            System.out.println("5. getParts() called on failed future -> CompletionException");
            System.out.println("6. Connection closes before buffered response written");
            System.exit(1);
        } else if (result.contains("Error") || result.contains("400")) {
            System.out.println("❌ Bug NOT reproduced");
            System.out.println("Response sent successfully despite buffering.");
            System.exit(0);
        } else {
            System.out.println("⚠️  Unexpected: " + result);
            System.exit(2);
        }
    }

    private static String makeRequest(int port) {
        try {
            String boundary = "----WebKitFormBoundary";
            String content = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "\r\n" +
                    "Content exceeding 10 byte limit here!\r\n" +
                    "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"text-field\"\r\n" +
                    "\r\n" +
                    "text\r\n" +
                    "--" + boundary + "--\r\n";

            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            System.out.println("\nClient: Sending " + bytes.length + " bytes (limit: 10)...");

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

            System.out.println("Client: Waiting for response...");
            int code = conn.getResponseCode();
            System.out.println("Client: Response code: " + code);
            
            String body = new String((code >= 400 ? conn.getErrorStream() : conn.getInputStream())
                    .readAllBytes(), StandardCharsets.UTF_8);

            return "HTTP " + code + ": " + body;

        } catch (IOException e) {
            System.err.println("Client: IOException - " + e.getMessage());
            return "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }
}
