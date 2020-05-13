/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    val app = Javalin.create().start(7070)

    app.get("/image/:color/width/:width/height/:height") { ctx ->
        // rrggbb, in hex, then setting the alpha channel to 0xff
        val colorStr = ctx.pathParam("color")
        val colorBits = colorStr.toInt(16)
        val color = (colorBits and 0xffffff) or 0xff000000.toInt()
        val width = ctx.pathParam("width").toInt()
        val height = ctx.pathParam("height").toInt()

        val image = java.awt.image.BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until height)
            for (x in 0 until width)
                image.setRGB(x, y, color)

        val os = ByteArrayOutputStream()
        val success = ImageIO.write(image, "png", os)
        val bytes = os.toByteArray()

        ctx.contentType("image/png").result(bytes)
    }

    Javalin.log.info("Red square: http://localhost:7070/image/ff0000/width/400/height/400")
    Javalin.log.info("Brown rectangle: http://localhost:7070/image/804020/width/400/height/100")
}
