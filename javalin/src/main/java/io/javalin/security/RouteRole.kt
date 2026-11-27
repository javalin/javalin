/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.security

import io.javalin.router.EndpointMetadata

/**
 * Marker interface for roles used in route declarations.
 * @see io.javalin.http.Context.routeRoles
 */
interface RouteRole

/**
 * List of roles used in route declaration
 */
data class Roles(val roles: Set<RouteRole>) : EndpointMetadata
