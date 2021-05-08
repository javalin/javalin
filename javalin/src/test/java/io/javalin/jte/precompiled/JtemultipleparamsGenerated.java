package io.javalin.jte.precompiled;

public final class JtemultipleparamsGenerated {
    public static final String JTE_NAME = "multiple-params.jte";
    public static final int[] JTE_LINE_INFO = {0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 2, 3};

    public static void render(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, String one, String two) {
        jteOutput.writeContent("<h1>");
        jteOutput.setContext("h1", null);
        jteOutput.writeUserContent(one);
        jteOutput.writeContent(" ");
        jteOutput.setContext("h1", null);
        jteOutput.writeUserContent(two);
        jteOutput.writeContent("!</h1>\n");
    }

    public static void renderMap(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, java.util.Map<String, Object> params) {
        String one = (String) params.get("one");
        String two = (String) params.get("two");
        render(jteOutput, jteHtmlInterceptor, one, two);
    }
}
