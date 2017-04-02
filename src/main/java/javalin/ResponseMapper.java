// Javalin - https://javalin.io
// Copyright 2017 David Åse
// Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE

package javalin;

import javalin.core.util.Util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResponseMapper {

    // TODO: Add GSON or other alternatives?

    public static String toJson(Object object) {
        if (Util.classExists("com.fasterxml.jackson.databind.ObjectMapper")) {
            try {
                return new ObjectMapper().writeValueAsString(object);
            } catch (JsonProcessingException e) {
                throw new HaltException(500, "Failed to write object as JSON");
            }
        } else {
            throw new HaltException(500, "No JSON-mapper available");
        }
    }

}
