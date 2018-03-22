/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.security;

import java.util.Arrays;
import java.util.List;

/**
 * Base interface for role used in {@link AccessManager}.
 *
 * @see <a href="https://javalin.io/documentation#access-manager">Access manager in docs</a>
 */
public interface Role {
    static List<Role> roles(Role... roles) {
        return Arrays.asList(roles);
    }
}
