/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import io.javalin.json.JavalinJackson;
import io.javalin.util.TestUtil;
import io.javalin.misc.UploadInfo;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestMultipartForms {

    private String EOL = System.getProperty("line.separator");

    private String TEXT_FILE_CONTENT = "This is my content." + EOL + "It's two lines." + EOL;

    // Using OkHttp because Unirest doesn't allow to send non-files as form-data
    private final OkHttpClient okHttp = new OkHttpClient();

    @Test
    public void test_upload_text() {
        new TestUtil().test((app, http) -> {
            app.post("/test-upload", ctx -> {
                ctx.result(IOUtils.toString(ctx.uploadedFile("upload").getContent(), StandardCharsets.UTF_8));
            });
            HttpResponse<String> response = http.post("/test-upload")
                .field("upload", new File("src/test/resources/upload-test/text.txt"))
                .asString();
            assertThat(response.getBody(), is(TEXT_FILE_CONTENT));
        });
    }

    @Test
    public void test_upload_mp3() {
        new TestUtil().test((app, http) -> {
            app.post("/test-upload", ctx -> {
                UploadedFile uf = ctx.uploadedFile("upload");
                ctx.json(new UploadInfo(uf.getName(), uf.getContent().available(), uf.getContentType(), uf.getExtension()));
            });
            File uploadFile = new File("src/test/resources/upload-test/sound.mp3");
            HttpResponse<String> response = http.post("/test-upload")
                .field("upload", uploadFile)
                .asString();
            UploadInfo uploadInfo = JavalinJackson.INSTANCE.fromJson(response.getBody(), UploadInfo.class);
            assertThat(uploadInfo.getContentLength(), is(uploadFile.length()));
            assertThat(uploadInfo.getFilename(), is(uploadFile.getName()));
            assertThat(uploadInfo.getContentType(), is("application/octet-stream"));
            assertThat(uploadInfo.getExtension(), is(".mp3"));
        });
    }

    @Test
    public void test_upload_png() {
        new TestUtil().test((app, http) -> {
            app.post("/test-upload", ctx -> {
                UploadedFile uf = ctx.uploadedFile("upload");
                ctx.json(new UploadInfo(uf.getName(), uf.getContent().available(), uf.getContentType(), uf.getExtension()));
            });
            File uploadFile = new File("src/test/resources/upload-test/image.png");
            HttpResponse<String> response = http.post("/test-upload")
                .field("upload", uploadFile, "image/png")
                .asString();
            UploadInfo uploadInfo = JavalinJackson.INSTANCE.fromJson(response.getBody(), UploadInfo.class);
            assertThat(uploadInfo.getContentLength(), is(uploadFile.length()));
            assertThat(uploadInfo.getFilename(), is(uploadFile.getName()));
            assertThat(uploadInfo.getContentType(), is("image/png"));
            assertThat(uploadInfo.getExtension(), is(".png"));
        });
    }

    @Test
    public void test_multipleFiles() {
        new TestUtil().test((app, http) -> {
            app.post("/test-upload", ctx -> ctx.result(String.valueOf(ctx.uploadedFiles("upload").size())));
            HttpResponse<String> response = http.post("/test-upload")
                .field("upload", new File("src/test/resources/upload-test/image.png"))
                .field("upload", new File("src/test/resources/upload-test/sound.mp3"))
                .field("upload", new File("src/test/resources/upload-test/text.txt"))
                .asString();
            assertThat(response.getBody(), is("3"));
        });
    }

    @Test
    public void test_doesntCrash_whenNotMultipart() {
        new TestUtil().test((app, http) -> {
            app.post("/test-upload", ctx -> {
                ctx.uploadedFile("non-existing-file");
                ctx.result("OK");
            });
            assertThat(http.post("/test-upload").asString().getBody(), is("OK"));
        });
    }

    @Test
    public void test_files_and_fields() {
        new TestUtil().test((app, http) -> {
            app.post("/test-upload", ctx -> ctx.result(ctx.formParam("field") + " and " + ctx.uploadedFile("upload").getName()));
            HttpResponse<String> response = http.post("/test-upload")
                .field("upload", new File("src/test/resources/upload-test/image.png"))
                .field("field", "text-value")
                .asString();
            assertThat(response.getBody(), is("text-value and image.png"));
        });
    }

    @Test
    public void test_files_and_multiple_fields() {
        new TestUtil().test((app, http) -> {
            app.post("/test-upload", ctx -> ctx.result(ctx.formParam("field") + " and " + ctx.formParam("field2")));
            HttpResponse<String> response = http.post("/test-upload")
                .field("upload", new File("src/test/resources/upload-test/image.png"))
                .field("field", "text-value")
                .field("field2", "text-value-2")
                .asString();
            assertThat(response.getBody(), is("text-value and text-value-2"));
        });
    }

    @Test
    public void test_unicodeTextFields() {
        new TestUtil().test((app, http) -> {
            app.post("/test-upload", ctx -> ctx.result(ctx.formParam("field") + " and " + ctx.uploadedFile("upload").getName()));
            HttpResponse<String> response = http.post("/test-upload")
                .field("upload", new File("src/test/resources/upload-test/text.txt"))
                .field("field", "♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟")
                .asString();
            assertThat(response.getBody(), is("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟ and text.txt"));
        });
    }

    @Test
    public void test_textFields() {
        new TestUtil().test((app, http) -> {
            app.post("/test-multipart-text-fields", ctx -> {
                List<String> foos = ctx.formParams("foo");
                List<String> foosExtractedManually = ctx.formParamMap().get("foo");
                String bar = ctx.formParam("bar");
                String baz = ctx.formParam("baz", "default");
                ctx.result("foos match: " + Objects.equals(foos, foosExtractedManually) + "\n"
                    + "foo: " + String.join(", ", foos) + "\n"
                    + "bar: " + bar + "\n"
                    + "baz: " + baz
                );
            });
            String responseAsString = okHttp.newCall(
                new Request.Builder().url(http.origin + "/test-multipart-text-fields").post(
                    new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("foo", "foo-1")
                        .addFormDataPart("bar", "bar-1")
                        .addFormDataPart("foo", "foo-2")
                        .build()
                ).build()
            ).execute().body().string();
            String expectedContent = "foos match: true" + "\n"
                + "foo: foo-1, foo-2" + "\n"
                + "bar: bar-1" + "\n"
                + "baz: default";
            assertThat(responseAsString, is(expectedContent));
        });
    }

    @Test
    public void test_fileAndTextFields() {
        String prefix = "PREFIX: ";
        File tempFile = new File("src/test/resources/upload-test/text.txt");
        new TestUtil().test((app, http) -> {
            app.post("/test-multipart-file-and-text", ctx -> {
                String fileContent = IOUtils.toString(ctx.uploadedFile("upload").getContent(), StandardCharsets.UTF_8);
                ctx.result(ctx.formParam("prefix") + fileContent);
            });
            String responseAsString = okHttp.newCall(
                new Request.Builder().url(http.origin + "/test-multipart-file-and-text").post(
                    new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("prefix", prefix)
                        .addFormDataPart("upload", tempFile.getName(), RequestBody.create(MediaType.parse("text/plain"), tempFile))
                        .build()
                ).build()
            ).execute().body().string();
            assertThat(responseAsString, is(prefix + TEXT_FILE_CONTENT));
        });
    }

}
