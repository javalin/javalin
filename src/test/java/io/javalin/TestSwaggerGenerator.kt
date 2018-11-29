/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.util.SwaggerGenerator

/**
 * My very nice description.
 * It can be multiple lines,
 * because why not!
 *
 * @path /users/:user-id
 * @pathParam user-id a very nice description
 * @queryParam my-query-param some other description
 * @formParam my-form-param a terrible description
 * @result SerializeableObject object
 */

/**
 * My other nice description. It's just a single line.
 *
 * @path /users/:user-id
 * @pathParam user-id a very nice description
 * @queryParam my-query-param some other description
 * @formParam my-form-param a terrible description
 * @result SerializeableObject object
 */

fun main(args: Array<String>) {
    SwaggerGenerator.generateFromSourceFiles()
}
