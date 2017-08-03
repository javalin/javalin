/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import org.apache.commons.io.FileUtils
import java.io.File

fun main(args: Array<String>) {

    val app = Javalin.create().setPort(7000).start()

    app.get("/") { ctx ->
        ctx.html(
                """
                    <form method='post' enctype='multipart/form-data'>
                        <input type='file' name='files' multiple>
                        <button>Upload</button>
                    </form>
                """
        )
    }

    app.post("/") { ctx ->
        ctx.uploadedFiles("files").forEach { (_, content, name) ->
            FileUtils.copyInputStreamToFile(content, File("upload/" + name))
        }
    }

}
