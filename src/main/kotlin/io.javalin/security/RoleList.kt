/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.security

import java.util.*

object RoleList {
    @JvmStatic fun roles(vararg roles: Role): List<Role> {
        return Arrays.asList(*roles)
    }
}
