/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.javalin.json.JavalinJackson;
import io.javalin.util.UploadInfo;
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
    public void test_upload_text() throws Exception {
        Javalin app = Javalin.start(0);
        app.post("/test-upload", ctx -> {
            ctx.result(IOUtils.toString(ctx.uploadedFile("upload").getContent(), StandardCharsets.UTF_8));
        });
        HttpResponse<String> response = Unirest.post("http://localhost:" + app.port() + "/test-upload")
            .field("upload", new File("src/test/resources/upload-test/text.txt"))
            .asString();
        assertThat(response.getBody(), is(TEXT_FILE_CONTENT));
        app.stop();
    }

    @Test
    public void test_upload_mp3() throws Exception {
        Javalin app = Javalin.start(0);
        app.post("/test-upload", ctx -> {
            UploadedFile uf = ctx.uploadedFile("upload");
            ctx.json(new UploadInfo(uf.getName(), uf.getContent().available(), uf.getContentType(), uf.getExtension()));
        });
        File uploadFile = new File("src/test/resources/upload-test/sound.mp3");
        HttpResponse<String> response = Unirest.post("http://localhost:" + app.port() + "/test-upload")
            .field("upload", uploadFile)
            .asString();
        UploadInfo uploadInfo = JavalinJackson.INSTANCE.toObject(response.getBody(), UploadInfo.class);
        assertThat(uploadInfo.getContentLength(), is(uploadFile.length()));
        assertThat(uploadInfo.getFilename(), is(uploadFile.getName()));
        assertThat(uploadInfo.getContentType(), is("application/octet-stream"));
        assertThat(uploadInfo.getExtension(), is(".mp3"));
        app.stop();
    }

    @Test
    public void test_upload_png() throws Exception {
        Javalin app = Javalin.start(0);
        app.post("/test-upload", ctx -> {
            UploadedFile uf = ctx.uploadedFile("upload");
            ctx.json(new UploadInfo(uf.getName(), uf.getContent().available(), uf.getContentType(), uf.getExtension()));
        });
        File uploadFile = new File("src/test/resources/upload-test/image.png");
        HttpResponse<String> response = Unirest.post("http://localhost:" + app.port() + "/test-upload")
            .field("upload", uploadFile, "image/png")
            .asString();
        UploadInfo uploadInfo = JavalinJackson.INSTANCE.toObject(response.getBody(), UploadInfo.class);
        assertThat(uploadInfo.getContentLength(), is(uploadFile.length()));
        assertThat(uploadInfo.getFilename(), is(uploadFile.getName()));
        assertThat(uploadInfo.getContentType(), is("image/png"));
        assertThat(uploadInfo.getExtension(), is(".png"));
        app.stop();
    }

    @Test
    public void test_multipleFiles() throws Exception {
        Javalin app = Javalin.start(0);
        app.post("/test-upload", ctx -> ctx.result(String.valueOf(ctx.uploadedFiles("upload").size())));
        HttpResponse<String> response = Unirest.post("http://localhost:" + app.port() + "/test-upload")
            .field("upload", new File("src/test/resources/upload-test/image.png"))
            .field("upload", new File("src/test/resources/upload-test/sound.mp3"))
            .field("upload", new File("src/test/resources/upload-test/text.txt"))
            .asString();
        assertThat(response.getBody(), is("3"));
        app.stop();
    }

    @Test
    public void test_doesntCrash_whenNotMultipart() throws Exception {
        Javalin app = Javalin.start(0);
        app.post("/test-upload", ctx -> {
            ctx.uploadedFile("non-existing-file");
            ctx.result("OK");
        });
        HttpResponse<String> response = Unirest.post("http://localhost:" + app.port() + "/test-upload").asString();
        assertThat(response.getBody(), is("OK"));
        app.stop();
    }

    @Test
    public void test_files_and_fields() throws Exception {
        Javalin app = Javalin.start(0);
        app.post("/test-upload", ctx -> ctx.result(ctx.formParam("field") + " and " + ctx.uploadedFile("upload").getName()));
        HttpResponse<String> response = Unirest.post("http://localhost:" + app.port() + "/test-upload")
            .field("upload", new File("src/test/resources/upload-test/image.png"))
            .field("field", "text-value")
            .asString();
        assertThat(response.getBody(), is("text-value and image.png"));
        app.stop();
    }

    @Test
    public void test_files_and_multiple_fields() throws Exception {
        Javalin app = Javalin.start(0);
        app.post("/test-upload", ctx -> ctx.result(ctx.formParam("field") + " and " + ctx.formParam("field2")));
        HttpResponse<String> response = Unirest.post("http://localhost:" + app.port() + "/test-upload")
            .field("upload", new File("src/test/resources/upload-test/image.png"))
            .field("field", "text-value")
            .field("field2", "text-value-2")
            .asString();
        assertThat(response.getBody(), is("text-value and text-value-2"));
        app.stop();
    }

    @Test
    public void test_unicodeTextFields() throws Exception {
        Javalin app = Javalin.start(0);
        app.post("/test-upload", ctx -> ctx.result(ctx.formParam("field") + " and " + ctx.uploadedFile("upload").getName()));
        HttpResponse<String> response = Unirest.post("http://localhost:" + app.port() + "/test-upload")
            .field("upload", new File("src/test/resources/upload-test/text.txt"))
            .field("field", "♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟")
            .asString();
        assertThat(response.getBody(), is("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟ and text.txt"));
        app.stop();
    }

    @Test
    public void test_textFields() throws Exception {
        Javalin app = Javalin.start(0);
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

        RequestBody body = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("foo", "foo-1")
            .addFormDataPart("bar", "bar-1")
            .addFormDataPart("foo", "foo-2")
            .build();

        Request request = new Request.Builder().url("http://localhost:" + app.port() + "/test-multipart-text-fields").post(body).build();

        String responseAsString = okHttp.newCall(request).execute().body().string();

        String expectedContent = "foos match: true" + "\n"
            + "foo: foo-1, foo-2" + "\n"
            + "bar: bar-1" + "\n"
            + "baz: default";

        assertThat(responseAsString, is(expectedContent));

        app.stop();
    }

    @Test
    public void test_fileAndTextFields() throws Exception {
        Javalin app = Javalin.start(0);
        app.post("/test-multipart-file-and-text", ctx -> {
            String prefix = ctx.formParam("prefix");
            String fileContent = IOUtils.toString(ctx.uploadedFile("upload").getContent(), StandardCharsets.UTF_8);
            ctx.result(prefix + fileContent);
        });

        String prefix = "PREFIX: ";

        File tempFile = new File("src/test/resources/upload-test/text.txt");

        RequestBody body = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("prefix", prefix)
            .addFormDataPart("upload", tempFile.getName(), RequestBody.create(MediaType.parse("text/plain"), tempFile))
            .build();

        Request request = new Request.Builder().url("http://localhost:" + app.port() + "/test-multipart-file-and-text").post(body).build();

        String responseAsString = okHttp.newCall(request).execute().body().string();

        assertThat(responseAsString, is(prefix + TEXT_FILE_CONTENT));

        app.stop();
    }

}
