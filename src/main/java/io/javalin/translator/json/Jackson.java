/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.translator.json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.HaltException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Jackson {

    private static Logger log = LoggerFactory.getLogger(Jackson.class);

    private static ObjectMapper objectMapper;

    public static String toJson(Object object) {
        if (objectMapper == null) {
            objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            String message = "Failed to write object as JSON";
            log.warn(message, e);
            throw new HaltException(500, message);
        }
    }

    public static <T> T toObject(String json, Class<T> clazz) {
        if (objectMapper == null) {
            objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            String message = "Failed to convert JSON to " + clazz.getName();
            log.warn(message, e);
            throw new HaltException(500, message);
        }
    }
}
