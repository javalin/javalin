/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.core

import io.javalin.Handler

class HandlerEntry(val type: Handler.Type, val path: String, val handler: Handler)
