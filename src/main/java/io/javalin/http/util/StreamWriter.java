package io.javalin.http.util;

import io.javalin.core.util.Header;
import io.javalin.http.Context;
import java.io.IOException;
import java.io.InputStream;

public class StreamWriter {

    private static int chunkSize = 256_000;

    public static void write(Context ctx, InputStream inputStream, String contentType) throws IOException {
        if (ctx.header(Header.RANGE) == null) {
            ctx.result(inputStream);
            return;
        }
        int fileLength = inputStream.available();
        String[] ranges = ctx.header(Header.RANGE).split("=")[1].split("-");
        int from = Integer.parseInt(ranges[0]);
        int to = chunkSize + from;
        if (to > fileLength) {
            to = fileLength - 1;
        }
        if (ranges.length == 2) {
            to = Integer.parseInt(ranges[1]);
        }
        int length = to - from + 1;
        ctx.status(206);
        ctx.header(Header.ACCEPT_RANGES, "bytes");
        ctx.header(Header.CONTENT_TYPE, contentType);
        ctx.header(Header.CONTENT_RANGE, String.format("bytes %d-%d/%d", from, to, fileLength));
        ctx.header(Header.CONTENT_LENGTH, "" + length);
        inputStream.skip(from);
        byte[] buffer = new byte[1024];
        try {
            while (length != 0) {
                int read = inputStream.read(buffer, 0, Math.min(buffer.length, length));
                ctx.res.getOutputStream().write(buffer, 0, read);
                length -= read;
            }
        } finally {
            inputStream.close();
        }
    }

}
