package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestFileUpload {

    private String EXPECTED_CONTENT = "Expected file content";

    @Test
    public void testFileUpload() throws Exception {

        Javalin app = Javalin.create().port(7737).start();

        app.post("/", ctx -> {
            File uploadedFile = ctx.uploadedFile("upload").getFile();
            ctx.result(FileUtils.readFileToString(uploadedFile));
        });

        HttpResponse<String> response = Unirest.post("http://localhost:7737/")
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
