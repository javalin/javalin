package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.core.compression.DynamicCompressionStrategy;
import java.util.*;

public class HelloWorldCompression {

    public static void main(String[] args) {
        boolean brotliEnabled = true;
        boolean gzipEnabled = true;

        Javalin app = Javalin.create(config -> {
            config.inner.dynamicCompressionStrategy = new DynamicCompressionStrategy(brotliEnabled, gzipEnabled);
        }).start(7070);
        app.get("/huge", ctx -> ctx.result(getSomeObjects(1000).toString()));
        app.get("/medium", ctx -> ctx.result(getSomeObjects(200).toString()));
        app.get("/tiny", ctx -> ctx.result(getSomeObjects(10).toString()));
    }

    private static ArrayList<SillyObject> getSomeObjects(int numObjects) {
        ArrayList<SillyObject> objects = new ArrayList<>();
        for(int i=0; i<numObjects; i++) {
            String val = "f" + (i + 1);
            SillyObject o = new SillyObject(val, val, val);
            objects.add(o);
        }
        return objects;
    }

    private static class SillyObject {
        private String f1, f2, f3;

        SillyObject(String fieldOne, String fieldTwo, String fieldThree) {
            this.f1 = fieldOne;
            this.f2 = fieldTwo;
            this.f3 = fieldThree;
        }

        @Override
        public String toString() {
            return "SillyObject(fieldOne=" + f1 + ", fieldTwo=" + f2 + ", fieldThree=" + f3 + ")";
        }
    }
}

