/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.staticfiles

/**
 * Interface for checking if symbolic links and aliases should be allowed.
 * This is a Jetty-free alternative to org.eclipse.jetty.server.AliasCheck.
 */
interface AliasCheck {
    /**
     * Check if an alias/symbolic link should be allowed.
     * 
     * @param path The resource path being requested
     * @param resource The resource being served
     * @return true if the alias should be allowed, false otherwise
     */
    fun checkAlias(path: String, resource: JavalinResource): Boolean
}

/**
 * Allows all aliases/symbolic links without any security checks.
 * Use with caution as this may expose files outside the intended directory.
 */
object AllowAllAliasCheck : AliasCheck {
    override fun checkAlias(path: String, resource: JavalinResource): Boolean = true
}

/**
 * Denies all aliases/symbolic links for maximum security.
 * This is the default behavior when no alias check is configured.
 */
object DenyAllAliasCheck : AliasCheck {
    override fun checkAlias(path: String, resource: JavalinResource): Boolean = false
}