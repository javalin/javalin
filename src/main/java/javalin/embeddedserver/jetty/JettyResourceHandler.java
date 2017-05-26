// Javalin - https://javalin.io
// Copyright 2017 David Åse
// Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE

package javalin.embeddedserver.jetty;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javalin.embeddedserver.StaticResourceHandler;

public class JettyResourceHandler implements StaticResourceHandler {

    private static Logger log = LoggerFactory.getLogger(JettyResourceHandler.class);

    private boolean initialized = false;
    private ResourceHandler resourceHandler = new ResourceHandler();

    public JettyResourceHandler(String staticFileDirectory) {
        if (staticFileDirectory != null) {
            resourceHandler.setResourceBase(Resource.newClassPathResource(staticFileDirectory).toString());
            resourceHandler.setDirAllowed(false);
            resourceHandler.setEtags(true);
            resourceHandler.setCacheControl("no-store,no-cache,must-revalidate");
            try {
                resourceHandler.start();
                initialized = true;
            } catch (Exception e) {
                log.error("Exception occurred starting static resource handler", e);
            }
        }
    }

    public boolean handle(HttpServletRequest request, HttpServletResponse response) {
        if (initialized) {
            String target = (String) request.getAttribute("jetty-target");
            Request baseRequest = (Request) request.getAttribute("jetty-request");
            try {
                if (!resourceHandler.getResource(target).isDirectory()) {
                    resourceHandler.handle(target, baseRequest, request, response);
                } else if (resourceHandler.getResource(target + "index.html").exists()) {
                    resourceHandler.handle(target, baseRequest, request, response);
                }
            } catch (IOException | ServletException e) {
                log.error("Exception occurred while handling static resource", e);
            }
            return baseRequest.isHandled();
        }
        return false;
    }

}
