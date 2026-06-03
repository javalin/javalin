package io.javalin;

import io.avaje.jsonb.Json;
import io.javalin.testing.SerializableObject;

// Importing the necessary test classes from Java so that the annotations are processed by avaje-jsonb
@Json.Import(SerializableObject.class)
@Json.Import(TestJsonMapper.Companion.Foo.class)

public class TestJavalinJsonb_Java {
  // All tests are implemented in TestJavalinJsonb.kt
}
