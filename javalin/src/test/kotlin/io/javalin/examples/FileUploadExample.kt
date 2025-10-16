/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import io.javalin.util.FileUtil

fun main() {

    Javalin.create {
        it.routes.get("/") { ctx ->
            ctx.html(
                """
                    <form method='post' enctype='multipart/form-data'>
                        <input type='file' name='files' multiple>
                        <button>Upload</button>
                    </form>
                """
            )
        }
        it.routes.post("/") { ctx ->
            ctx.uploadedFiles("files").forEach {
                FileUtil.streamToFile(it.content(), "upload/${it.filename()}")
            }
        }
    }.start(7070)

}
