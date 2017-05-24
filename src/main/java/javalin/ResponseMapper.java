// Javalin - https://javalin.io
// Copyright 2017 David Åse
// Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE

package javalin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javalin.core.util.Util;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ResponseMapper {

    private static Logger log = LoggerFactory.getLogger(ResponseMapper.class);

    // TODO: Add GSON or other alternatives?

    public static String toJson(Object object) {
        if (Util.classExists("com.fasterxml.jackson.databind.ObjectMapper")) {
            try {
                return new ObjectMapper().writeValueAsString(object);
            } catch (Exception e) {
                String message = "Failed to write object as JSON";
                log.warn(message, e);
                throw new HaltException(500, message);
            }
        } else {
            String message = "Jackson dependency missing. "
                + "Please add Jackson to your POM to use automatic json-mapping: "
                + "https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind";
            log.warn(message);
            throw new HaltException(500, message);
        }
    }

}
