/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.javalin.translator.json.JavalinJacksonPlugin;
import io.javalin.util.UploadInfo;
import java.io.File;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestMultipartForms {

    private String EOL = System.getProperty("line.separator");

    @Test
    public void test_upload_text() throws Exception {
        Javalin app = Javalin.start(0);
        app.post("/test-upload", ctx -> {
            ctx.result(IOUtils.toString(ctx.uploadedFile("upload").getContent()));
        });
        HttpResponse<String> response = Unirest.post("http://localhost:" + app.port() + "/test-upload")
            .field("upload", new File("src/test/resources/upload-test/text.txt"))
            .asString();
        assertThat(response.getBody(), is("This is my content." + EOL + "It's two lines." + EOL));
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
        UploadInfo uploadInfo = JavalinJacksonPlugin.INSTANCE.toObject(response.getBody(), UploadInfo.class);
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
        UploadInfo uploadInfo = JavalinJacksonPlugin.INSTANCE.toObject(response.getBody(), UploadInfo.class);
        assertThat(uploadInfo.getContentLength(), is(uploadFile.length()));
        assertThat(uploadInfo.getFilename(), is(uploadFile.getName()));
        assertThat(uploadInfo.getContentType(), is("image/png"));
        assertThat(uploadInfo.getExtension(), is(".png"));
        app.stop();
    }

}
