package io.javalin.http.sse;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;

class Emitter {

    private static final String newline = "\n";

    private final AsyncContext asyncContext;
    private ServletOutputStream output;
    private boolean close = false;

    public Emitter(AsyncContext asyncContext) {
        this.asyncContext = asyncContext;
        try {
            this.output = asyncContext.getResponse().getOutputStream();
        } catch (IOException e) {
            close = true;
        }
    }

    public void emit(@NotNull String event,@NotNull String data, String id) {
        synchronized(this) {
            try {
                StringBuilder sb = new StringBuilder();
                if (id != null) {
                    sb.append("id: ").append(id).append(newline);
                }
                sb.append("event: ").append(event).append(newline);
                for (String line : data.split("\\r?\\n")) {
                    sb.append("data: ").append(line).append(newline);
                }
                sb.append(newline).append(newline);
                output.print(sb.toString());
                asyncContext.getResponse().flushBuffer();
            } catch (IOException e) {
                close = true;
            }
        }
    }

    public boolean isClose(){
      return close;
    }

}
