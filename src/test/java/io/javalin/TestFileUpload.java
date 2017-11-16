package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by enchanting on 10.08.17.
 */
public class TestFileUpload extends _UnirestBaseTest {

    private static final String EXPECTED_CONTENT = "Expected file content";

    @Test
    public void testFileUpload() throws Exception {

        app.post("/testFileUpload", ctx -> {
            File uploadedFile = createTempFile("Not expected content ...");
            FileUtils.copyInputStreamToFile(ctx.uploadedFile("upload").getContent(), uploadedFile);
            ctx.result(FileUtils.readFileToString(uploadedFile));
        });

        HttpResponse<String> response = Unirest.post("http://localhost:7777/testFileUpload")
            .field("upload", createTempFile(EXPECTED_CONTENT))
            .asString();

        assertThat(response.getBody(), is(EXPECTED_CONTENT));

        app.stop();
    }

    private File createTempFile(String content) throws IOException {
        File file = File.createTempFile("javalin-testfile-", ".tmp");
        FileUtils.writeStringToFile(file, content);
        file.deleteOnExit();
        return file;
    }

}
