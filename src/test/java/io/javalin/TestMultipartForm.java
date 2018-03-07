package io.javalin;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestMultipartForm extends _UnirestBaseTest {

    // Using OkHttp because Unirest doesn't allow to send non-files as form-data
    private final OkHttpClient okHttp = new OkHttpClient();

    @Test
    public void testMultipartFormTextFields() throws Exception {
        app.post("/testMultipartForm_text", ctx -> {
            List<String> foos = ctx.multipartFormParams("foo");
            List<String> foosExtractedManually = ctx.multipartFormParamMap().get("foo");

            String bar = ctx.multipartFormParam("bar");
            String baz = ctx.multipartFormParamOrDefault("baz", "default");

            ctx.result("foos match: " + foos.equals(foosExtractedManually) + "\n"
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

        Request request = new Request.Builder().url(_UnirestBaseTest.origin + "/testMultipartForm_text").post(body).build();

        String responseAsString = okHttp.newCall(request).execute().body().string();

        String expectedContent = "foos match: true" + "\n"
                + "foo: foo-1, foo-2" + "\n"
                + "bar: bar-1" + "\n"
                + "baz: default";

        assertThat(responseAsString, is(expectedContent));

        app.stop();
    }

}
