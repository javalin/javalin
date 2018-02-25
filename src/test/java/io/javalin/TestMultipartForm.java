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

    private static final String EXPECTED_CONTENT = "foo: foo-1, foo-2; bar: bar-1";

    @Test
    public void testSubmitMultipartForm() throws Exception {
        app.post("/testMultipartForm", ctx -> {
            List<String> foos = ctx.multipartFormParams("foo");
            String bar = ctx.multipartFormParam("bar");
            ctx.result(String.format("foo: %s, %s; bar: %s", foos.get(0), foos.get(1), bar));
        });

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("foo", "foo-1")
                .addFormDataPart("foo", "foo-2")
                .addFormDataPart("bar", "bar-1")
                .build();

        Request request = new Request.Builder().url(_UnirestBaseTest.origin + "/testMultipartForm").post(body).build();

        String responseAsString = okHttp.newCall(request).execute().body().string();

        assertThat(responseAsString, is(EXPECTED_CONTENT));

        app.stop();
    }

}
