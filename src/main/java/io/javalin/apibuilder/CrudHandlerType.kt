/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.apibuilder

enum class CrudHandlerType(val value: String) {
    GET_ALL("getAll"),
    GET_ONE("getOne"),
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete")
}
