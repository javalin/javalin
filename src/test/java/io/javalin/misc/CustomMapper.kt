/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.misc

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

class CustomMapper : ObjectMapper() {
    init {
        this.enable(SerializationFeature.INDENT_OUTPUT)
        this.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
    }
}
