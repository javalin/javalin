package io.javalin.jte.precompiled;

import io.javalin.TestTemplates.JteTestPage;

public final class JtetestGenerated {
    public static final String JTE_NAME = "test.jte";
    public static final int[] JTE_LINE_INFO = {0, 0, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 3};

    public static void render(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, JteTestPage page) {
        jteOutput.writeContent("<h1>");
        jteOutput.setContext("h1", null);
        jteOutput.writeUserContent(page.getHello());
        jteOutput.writeContent(" ");
        jteOutput.setContext("h1", null);
        jteOutput.writeUserContent(page.getWorld());
        jteOutput.writeContent("!</h1>\n");
    }

    public static void renderMap(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, java.util.Map<String, Object> params) {
        JteTestPage page = (JteTestPage) params.get("page");
        render(jteOutput, jteHtmlInterceptor, page);
    }
}
