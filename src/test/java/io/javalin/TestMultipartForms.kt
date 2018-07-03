/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.json.JavalinJackson
import io.javalin.misc.UploadInfo
import io.javalin.util.TestUtil
import okhttp3.*
import org.apache.commons.io.IOUtils
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets

class TestMultipartForms {

    private val EOL = System.getProperty("line.separator")
    private val TEXT_FILE_CONTENT = "This is my content." + EOL + "It's two lines." + EOL

    // Using OkHttp because Unirest doesn't allow to send non-files as form-data
    private val okHttp = OkHttpClient()

    @Test
    fun `text is uploaded correctly`() = TestUtil.test { app, http ->
        app.post("/test-upload") { ctx -> ctx.result(IOUtils.toString(ctx.uploadedFile("upload")!!.content, StandardCharsets.UTF_8)) }
        val response = http.post("/test-upload")
                .field("upload", File("src/test/resources/upload-test/text.txt"))
                .asString()
        assertThat(response.body, `is`(TEXT_FILE_CONTENT))
    }

    @Test
    fun `mp3s are uploaded correctly`() = TestUtil.test { app, http ->
        app.post("/test-upload") { ctx ->
            val uf = ctx.uploadedFile("upload")!!
            ctx.json(UploadInfo(uf.name, uf.content.available().toLong(), uf.contentType, uf.extension))
        }
        val uploadFile = File("src/test/resources/upload-test/sound.mp3")
        val response = http.post("/test-upload")
                .field("upload", uploadFile)
                .asString()
        val uploadInfo = JavalinJackson.fromJson(response.body, UploadInfo::class.java)
        assertThat(uploadInfo.contentLength, `is`(uploadFile.length()))
        assertThat(uploadInfo.filename, `is`(uploadFile.name))
        assertThat(uploadInfo.contentType, `is`("application/octet-stream"))
        assertThat(uploadInfo.extension, `is`(".mp3"))
    }

    @Test
    fun `pngs are uploaded correctly`() = TestUtil.test { app, http ->
        app.post("/test-upload") { ctx ->
            val uf = ctx.uploadedFile("upload")
            ctx.json(UploadInfo(uf!!.name, uf.content.available().toLong(), uf.contentType, uf.extension))
        }
        val uploadFile = File("src/test/resources/upload-test/image.png")
        val response = http.post("/test-upload")
                .field("upload", uploadFile, "image/png")
                .asString()
        val uploadInfo = JavalinJackson.fromJson(response.body, UploadInfo::class.java)
        assertThat(uploadInfo.contentLength, `is`(uploadFile.length()))
        assertThat(uploadInfo.filename, `is`(uploadFile.name))
        assertThat(uploadInfo.contentType, `is`("image/png"))
        assertThat(uploadInfo.extension, `is`(".png"))
    }

    @Test
    fun `multiple files are handled correctly`() = TestUtil.test { app, http ->
        app.post("/test-upload") { ctx -> ctx.result(ctx.uploadedFiles("upload").size.toString()) }
        val response = http.post("/test-upload")
                .field("upload", File("src/test/resources/upload-test/image.png"))
                .field("upload", File("src/test/resources/upload-test/sound.mp3"))
                .field("upload", File("src/test/resources/upload-test/text.txt"))
                .asString()
        assertThat(response.body, `is`("3"))
    }

    @Test
    fun `uploadedFile() doesn't throw for missing file`() = TestUtil.test { app, http ->
        app.post("/test-upload") { ctx ->
            ctx.uploadedFile("non-existing-file")
            ctx.result("OK")
        }
        assertThat(http.post("/test-upload").asString().body, `is`("OK"))
    }

    @Test
    fun `mixing files and text fields works`() = TestUtil.test { app, http ->
        app.post("/test-upload") { ctx -> ctx.result(ctx.formParam("field") + " and " + ctx.uploadedFile("upload")!!.name) }
        val response = http.post("/test-upload")
                .field("upload", File("src/test/resources/upload-test/image.png"))
                .field("field", "text-value")
                .asString()
        assertThat(response.body, `is`("text-value and image.png"))
    }

    @Test
    fun `mixing files and text fields works with multiple fields`() = TestUtil.test { app, http ->
        app.post("/test-upload") { ctx -> ctx.result(ctx.formParam("field") + " and " + ctx.formParam("field2")) }
        val response = http.post("/test-upload")
                .field("upload", File("src/test/resources/upload-test/image.png"))
                .field("field", "text-value")
                .field("field2", "text-value-2")
                .asString()
        assertThat(response.body, `is`("text-value and text-value-2"))
    }

    @Test
    fun `unicode text-fields work`() = TestUtil.test { app, http ->
        app.post("/test-upload") { ctx -> ctx.result(ctx.formParam("field") + " and " + ctx.uploadedFile("upload")!!.name) }
        val response = http.post("/test-upload")
                .field("upload", File("src/test/resources/upload-test/text.txt"))
                .field("field", "♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟")
                .asString()
        assertThat(response.body, `is`("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟ and text.txt"))
    }

    @Test
    fun `text-fields work`() = TestUtil.test { app, http ->
        app.post("/test-multipart-text-fields") { ctx ->
            val foosExtractedManually = ctx.formParamMap()["foo"]
            val foos = ctx.formParams("foo")
            val bar = ctx.formParam("bar")
            val baz = ctx.formParam("baz", "default")
            ctx.result("foos match: " + (foos == foosExtractedManually) + "\n"
                    + "foo: " + foos.joinToString(", ") + "\n"
                    + "bar: " + bar + "\n"
                    + "baz: " + baz
            )
        }
        val responseAsString = okHttp.newCall(
                Request.Builder().url(http.origin + "/test-multipart-text-fields").post(
                        MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("foo", "foo-1")
                                .addFormDataPart("bar", "bar-1")
                                .addFormDataPart("foo", "foo-2")
                                .build()
                ).build()
        ).execute().body()!!.string()
        val expectedContent = ("foos match: true" + "\n"
                + "foo: foo-1, foo-2" + "\n"
                + "bar: bar-1" + "\n"
                + "baz: default")
        assertThat(responseAsString, `is`(expectedContent))
    }

    @Test
    fun `fields and files work in other client`() = TestUtil.test { app, http ->
        val prefix = "PREFIX: "
        val tempFile = File("src/test/resources/upload-test/text.txt")
        app.post("/test-multipart-file-and-text") { ctx ->
            val fileContent = IOUtils.toString(ctx.uploadedFile("upload")!!.content, StandardCharsets.UTF_8)
            ctx.result(ctx.formParam("prefix")!! + fileContent)
        }
        val responseAsString = okHttp.newCall(
                Request.Builder().url(http.origin + "/test-multipart-file-and-text").post(
                        MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("prefix", prefix)
                                .addFormDataPart("upload", tempFile.name, RequestBody.create(MediaType.parse("text/plain"), tempFile))
                                .build()
                ).build()
        ).execute().body()!!.string()
        assertThat(responseAsString, `is`(prefix + TEXT_FILE_CONTENT))
    }

}
