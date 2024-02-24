/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.config.SizeUnit
import io.javalin.http.ContentType
import io.javalin.http.formParamAsClass
import io.javalin.http.util.MultipartUtil
import io.javalin.json.fromJsonString
import io.javalin.testing.TestUtil
import io.javalin.testing.UploadInfo
import io.javalin.testing.fasterJacksonMapper
import io.javalin.util.FileUtil
import jakarta.servlet.MultipartConfigElement
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class TestMultipartForms {

    private val LF = "\n"
    private val CRLF = "\r\n"
    private val TEXT_FILE_CONTENT_LF = "This is my content." + LF + "It's two lines." + LF
    private val TEXT_FILE_CONTENT_CRLF = "This is my content." + CRLF + "It's two lines." + CRLF

    // Using OkHttp because Unirest doesn't allow to send non-files as form-data
    private val okHttp = OkHttpClient()

    @Test
    fun `text is uploaded correctly`() = TestUtil.test { app, http ->
        app.post("/test-upload") {
            it.result(it.uploadedFile("upload")!!.contentAndClose { it.readBytes().toString(Charsets.UTF_8) })
        }
        val response = http.post("/test-upload")
            .field("upload", File("src/test/resources/upload-test/text.txt"))
            .asString()
        if (CRLF in response.body) {
            assertThat(response.body).isEqualTo(TEXT_FILE_CONTENT_CRLF)
        } else {
            assertThat(response.body).isEqualTo(TEXT_FILE_CONTENT_LF)
        }
    }

    @Test
    fun `mp3s are uploaded correctly`() = TestUtil.test { app, http ->
        app.post("/test-upload") { ctx ->
            val uf = ctx.uploadedFile("upload")!!
            ctx.json(UploadInfo(uf.filename(), uf.size(), uf.contentType(), uf.extension()))
        }
        val uploadFile = File("src/test/resources/upload-test/sound.mp3")
        val response = http.post("/test-upload")
            .field("upload", uploadFile)
            .asString()

        val uploadInfo = fasterJacksonMapper.fromJsonString<UploadInfo>(response.body)
        assertThat(uploadInfo.size).isEqualTo(uploadFile.length())
        assertThat(uploadInfo.filename).isEqualTo(uploadFile.name)
        assertThat(uploadInfo.contentType).isEqualTo(ContentType.OCTET_STREAM)
        assertThat(uploadInfo.extension).isEqualTo(".mp3")
    }

    @Test
    fun `pngs are uploaded correctly`() = TestUtil.test { app, http ->
        app.post("/test-upload") { ctx ->
            val uf = ctx.uploadedFile("upload")
            ctx.json(UploadInfo(uf!!.filename(), uf.size(), uf.contentType(), uf.extension()))
        }
        val uploadFile = File("src/test/resources/upload-test/image.png")
        val response = http.post("/test-upload")
            .field("upload", uploadFile, ContentType.IMAGE_PNG.mimeType)
            .asString()
        val uploadInfo = fasterJacksonMapper.fromJsonString<UploadInfo>(response.body)
        assertThat(uploadInfo.size).isEqualTo(uploadFile.length())
        assertThat(uploadInfo.filename).isEqualTo(uploadFile.name)
        assertThat(uploadInfo.contentType).isEqualTo(ContentType.IMAGE_PNG.mimeType)
        assertThat(uploadInfo.extension).isEqualTo(".png")
        assertThat(uploadInfo.size).isEqualTo(6690L)
    }

    @Test
    fun `multiple files are handled correctly`() = TestUtil.test { app, http ->
        app.post("/test-upload") { it.result(it.uploadedFiles("upload").size.toString()) }
        val response = http.post("/test-upload")
            .field("upload", File("src/test/resources/upload-test/image.png"))
            .field("upload", File("src/test/resources/upload-test/sound.mp3"))
            .field("upload", File("src/test/resources/upload-test/text.txt"))
            .asString()
        assertThat(response.body).isEqualTo("3")
    }

    @Test
    fun `uploadedFile doesn't throw for missing file`() = TestUtil.test { app, http ->
        app.post("/test-upload") { ctx ->
            ctx.uploadedFile("non-existing-file")
            ctx.result("OK")
        }
        assertThat(http.post("/test-upload").asString().body).isEqualTo("OK")
    }

    @Test
    fun `uploadedFileMap throws if body has already been consumed`() {
        val result = TestUtil.testWithResult { app, http ->
            app.post("/") { ctx ->
                ctx.body() // consume body
                ctx.uploadedFileMap() // try to consume body again
            }
            val response = http.post("/")
                .field("upload", File("src/test/resources/upload-test/image.png"))
                .asString()
            assertThat(response.body).isEqualTo("Server Error");
        }
        assertThat(result.logs).contains("Request body has already been consumed")
    }

    @Test
    fun `getting all files is handled correctly`() = TestUtil.test { app, http ->
        app.post("/test-upload") { ctx ->
            ctx.result(ctx.uploadedFiles().joinToString(", ") { it.filename() })
        }
        val response = http.post("/test-upload")
            .field("upload", File("src/test/resources/upload-test/image.png"))
            .field("upload", File("src/test/resources/upload-test/sound.mp3"))
            .field("upload", File("src/test/resources/upload-test/text.txt"))
            .field("text-field", "text")
            .asString()
        assertThat(response.body).isEqualTo("image.png, sound.mp3, text.txt")
    }

    @Test
    fun `custom multipart properties applied correctly`() = TestUtil.test { app, http ->
        app.before("/test-upload") { ctx ->
            ctx.attribute("org.eclipse.jetty.multipartConfig", MultipartConfigElement("/tmp", 10, 10, 5))
        }

        app.post("/test-upload") { ctx ->
            ctx.result(ctx.uploadedFiles().joinToString(", ") { it.filename() })
        }

        app.exception(IllegalStateException::class.java) { e, ctx ->
            ctx.result("${e::class.java.canonicalName} ${e.message}")
        }

        val response = http.post("/test-upload")
            .field("upload", File("src/test/resources/upload-test/image.png"))
            .field("text-field", "text")
            .asString()
        assertThat(response.body).isEqualTo("java.lang.IllegalStateException Request exceeds maxRequestSize (10)")
    }

    @Test
    fun `getting all files doesn't throw for non multipart request`() = TestUtil.test { app, http ->
        app.post("/test-upload") { it.result(it.uploadedFiles().joinToString("\n")) }

        val response = http.post("/test-upload")
            .header("content-type", ContentType.PLAIN)
            .body("")
            .asString()

        assertThat(response.body).isEqualTo("")
    }

    @Test
    fun `mixing files and text fields works`() = TestUtil.test { app, http ->
        app.post("/test-upload") { it.result(it.formParam("field") + " and " + it.uploadedFile("upload")!!.filename()) }
        val response = http.post("/test-upload")
            .field("upload", File("src/test/resources/upload-test/image.png"))
            .field("field", "text-value")
            .asString()
        assertThat(response.body).isEqualTo("text-value and image.png")
    }

    @Test
    fun `mixing files and text fields works with multiple fields`() = TestUtil.test { app, http ->
        app.post("/test-upload") { it.result(it.formParam("field") + " and " + it.formParam("field2")) }
        val response = http.post("/test-upload")
            .field("upload", File("src/test/resources/upload-test/image.png"))
            .field("field", "text-value")
            .field("field2", "text-value-2")
            .asString()
        assertThat(response.body).isEqualTo("text-value and text-value-2")
    }

    @Test
    fun `unicode text-fields work`() = TestUtil.test { app, http ->
        app.post("/test-upload") { it.result(it.formParam("field") + " and " + it.uploadedFile("upload")!!.filename()) }
        val response = http.post("/test-upload")
            .field("upload", File("src/test/resources/upload-test/text.txt"))
            .field("field", "♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟")
            .asString()
        assertThat(response.body).isEqualTo("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟ and text.txt")
    }

    @Test
    fun `text-fields work`() = TestUtil.test { app, http ->
        app.post("/test-multipart-text-fields") { ctx ->
            val foosExtractedManually = ctx.formParamMap()["foo"]
            val foos = ctx.formParams("foo")
            val bar = ctx.formParam("bar")
            val baz = ctx.formParamAsClass<String>("baz").getOrDefault("default")
            ctx.result(
                "foos match: " + (foos == foosExtractedManually) + "\n"
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
        ).execute().body!!.string()
        val expectedContent = ("foos match: true" + "\n"
            + "foo: foo-1, foo-2" + "\n"
            + "bar: bar-1" + "\n"
            + "baz: default")
        assertThat(responseAsString).isEqualTo(expectedContent)
    }

    @Test
    fun `fields and files work in other client`() = TestUtil.test { app, http ->
        val prefix = "PREFIX: "
        val tempFile = File("src/test/resources/upload-test/text.txt")
        app.post("/test-multipart-file-and-text") { ctx ->
            val fileContent = ctx.uploadedFile("upload")!!.contentAndClose { it.readBytes().toString(Charsets.UTF_8) }
            ctx.result(ctx.formParam("prefix")!! + fileContent)
        }
        val responseAsString = okHttp.newCall(
            Request.Builder().url(http.origin + "/test-multipart-file-and-text").post(
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("prefix", prefix)
                    .addFormDataPart("upload", tempFile.name, tempFile.asRequestBody(ContentType.PLAIN.toMediaTypeOrNull()))
                    .build()
            ).build()
        ).execute().body!!.string()

        if (CRLF in responseAsString) {
            assertThat(responseAsString).isEqualTo(prefix + TEXT_FILE_CONTENT_CRLF)
        } else {
            assertThat(responseAsString).isEqualTo(prefix + TEXT_FILE_CONTENT_LF)
        }
    }

    @Test
    fun `returning files as map works`() = TestUtil.test { app, http ->
        app.post("/test-upload-map") { ctx ->
            //get the uploaded files as a map and then sort them
            val files = ctx.uploadedFileMap().toSortedMap()

            //now turn that into a string.  we just need to know that we have correctly received the files since there
            //are other tests to check that the uploaded file is correct.  We therefore expect to receive back something
            //which looks like file1 --> image.png, file_array[] -> sound.mp3:text.txt
            val metadata = files
                .map { "${it.key} --> ${it.value.map { file -> file.filename() }.joinToString(":")}" }
                .joinToString(", ")

            ctx.result(metadata)
        }

        //post the data to the end point
        val response = http.post("/test-upload-map")
            .field("file1", File("src/test/resources/upload-test/image.png"))
            .field("file_array[]", File("src/test/resources/upload-test/sound.mp3"))
            .field("file_array[]", File("src/test/resources/upload-test/text.txt"))
            .asString()
            .body

        //create the expected response
        val expected = "file1 --> image.png, file_array[] --> sound.mp3:text.txt"

        //and verify it
        assertThat(response).isEqualTo(expected)
    }

    @Test
    fun `changing the multipart config correctly sets it`() = TestUtil.test { app, http ->
        //note: this test does not check the cache directory is set correctly as there is no way to know which
        //paths exist and are writable on the system the test is being run on.  However, if the other parameters
        //are read successfully
        app.unsafeConfig().also {
            it.jetty.multipartConfig.maxFileSize(100, SizeUnit.MB)
            it.jetty.multipartConfig.maxInMemoryFileSize(10, SizeUnit.MB)
            it.jetty.multipartConfig.maxTotalRequestSize(1, SizeUnit.GB)
        }

        app.post("/test-multipart-config") { ctx ->
            //get the files - this is solely required to ensure that the preUploadFunction has been called
            ctx.uploadedFiles()

            //now get hold of the MultipartConfigElement from the request attributes
            val config = ctx.attribute<MultipartConfigElement>(MultipartUtil.MULTIPART_CONFIG_ATTRIBUTE)!!

            ctx.result("${config.maxFileSize}:${config.fileSizeThreshold}:${config.maxRequestSize}")
        }

        //post the data to the end point
        val response = http.post("/test-multipart-config")
            .field("file1", File("src/test/resources/upload-test/image.png"))
            .asString()
            .body

        //create the expected response which are the various sizes (in bytes) separated by colons
        val expected = "${100 * 1024 * 1024}:${10 * 1024 * 1024}:${1 * 1024 * 1024 * 1024}"

        //and verify it
        assertThat(response).isEqualTo(expected)
    }

    @Test
    fun `fileutil works`() {
        val content = if (File.separatorChar == '\\') TEXT_FILE_CONTENT_CRLF else TEXT_FILE_CONTENT_LF
        val prefix = "src/test/resources/upload-test";
        FileUtil.readFile("$prefix/text.txt").let { assertThat(it).isEqualTo(content) }
        FileUtil.readResource("/upload-test/text.txt").let { assertThat(it).isEqualTo(content) }
        File("$prefix/text.txt").inputStream().use { inputStream ->
            FileUtil.streamToFile(inputStream, "$prefix/text-copy.txt")
        }
        val file = File("$prefix/text-copy.txt")
        assertThat(file.readText()).isEqualTo(content)
        file.deleteOnExit()
    }
}
