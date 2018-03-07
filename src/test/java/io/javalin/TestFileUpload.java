package io.javalin;

import java.io.File;
import java.io.IOException;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by enchanting on 10.08.17.
 */
public class TestFileUpload extends _UnirestBaseTest {

    private static final String EXPECTED_CONTENT = "Expected file content";

    private final OkHttpClient okHttp = new OkHttpClient();

    @Test
    public void testFileUpload() throws Exception {

        app.post("/testFileUpload", ctx -> {
            File uploadedFile = createTempFile("Not expected content ...");
            FileUtils.copyInputStreamToFile(ctx.uploadedFile("upload").getContent(), uploadedFile);
            ctx.result(FileUtils.readFileToString(uploadedFile));
        });

        HttpResponse<String> response = Unirest.post(_UnirestBaseTest.origin + "/testFileUpload")
            .field("upload", createTempFile(EXPECTED_CONTENT))
            .asString();

        assertThat(response.getBody(), is(EXPECTED_CONTENT));

        app.stop();
    }


    /**
     * @see TestMultipartForm
     */
    @Test
    public void testFileAndTextUpload() throws Exception {
        app.post("/testFileUpload_fileAndText", ctx -> {
            String prefix = ctx.multipartFormParam("prefix");
            File uploadedFile = createTempFile("Not expected content ...");
            FileUtils.copyInputStreamToFile(ctx.uploadedFile("upload").getContent(), uploadedFile);
            ctx.result(prefix + FileUtils.readFileToString(uploadedFile));
        });

        String prefix = "PREFIX: ";

        File tempFile = createTempFile(EXPECTED_CONTENT);

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("prefix", prefix)
                .addFormDataPart("upload", tempFile.getName(), RequestBody.create(MediaType.parse("text/plain"), tempFile))
                .build();

        Request request = new Request.Builder().url(_UnirestBaseTest.origin + "/testFileUpload_fileAndText").post(body).build();

        String responseAsString = okHttp.newCall(request).execute().body().string();

        assertThat(responseAsString, is(prefix + EXPECTED_CONTENT));

        app.stop();
    }

    private File createTempFile(String content) throws IOException {
        File file = File.createTempFile("javalin-testfile-", ".tmp");
        FileUtils.writeStringToFile(file, content);
        file.deleteOnExit();
        return file;
    }

}
