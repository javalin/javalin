package io.javalin.testing

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.plugin.json.JavalinJackson

val fasterJacksonMapper = JavalinJackson(ObjectMapper())
