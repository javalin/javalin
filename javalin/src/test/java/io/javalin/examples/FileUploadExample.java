/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.util.FileUtil;
import static io.javalin.testing.JavalinTestUtil.*;

public class FileUploadExample {

    public static void main(String[] args) {

        Javalin app = Javalin.create().start(7000);

        get(app, "/", ctx ->
            ctx.html(
                ""
                    + "<form method='post' enctype='multipart/form-data'>"
                    + "    <input type='file' name='files' multiple>"
                    + "    <button>Upload</button>"
                    + "</form>"
            )
        );

        post(app, "/", ctx -> {
            ctx.uploadedFiles("files").forEach(file -> {
                FileUtil.streamToFile(file.content(), "upload/" + file.filename());
            });
        });

    }

}
