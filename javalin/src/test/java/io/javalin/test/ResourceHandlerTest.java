package io.javalin.test;

import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import java.lang.reflect.Method;

/**
 * Test to understand Jetty 12 ResourceHandler capabilities
 */
public class ResourceHandlerTest {
    public static void main(String[] args) {
        try {
            ResourceHandler rh = new ResourceHandler();
            
            System.out.println("=== Available ResourceHandler Methods ===");
            Method[] methods = ResourceHandler.class.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().startsWith("set") || method.getName().startsWith("is") || 
                    method.getName().startsWith("get") || method.getName().contains("welcome") ||
                    method.getName().contains("etag") || method.getName().contains("mime")) {
                    System.out.println(method.toString());
                }
            }
            
            System.out.println("\n=== Default Configuration ===");
            try {
                System.out.println("Welcome Files: " + rh.getWelcomeFiles());
                System.out.println("ETags enabled: " + rh.isEtags());
                System.out.println("Directory allowed: " + rh.isDirAllowed());
                System.out.println("Accept ranges: " + rh.isAcceptRanges());
            } catch (Exception e) {
                System.out.println("Error accessing properties: " + e.getMessage());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}