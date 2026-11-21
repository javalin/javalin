/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import java.net.URI
import java.util.Collections

/**
 * A [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html) problem details response.
 *
 * When sent through [Context.problem], [additionalMembers] are serialized as top-level extension
 * members. This model is independent of the configured JSON mapper so it can also be used by
 * future serializers.
 */
class ProblemResponse @JvmOverloads constructor(
    /** A short, human-readable summary of the problem type. */
    val title: String,
    /** The HTTP status code for this problem occurrence. When set, [Context.problem] applies it to the response. */
    val status: Int? = null,
    /** A URI reference identifying the problem type. */
    val type: URI = URI.create("about:blank"),
    /** A human-readable explanation specific to this occurrence. */
    val detail: String? = null,
    /** A URI reference identifying this specific problem occurrence. */
    val instance: URI? = null,
    additionalMembers: Map<String, Any?> = emptyMap(),
) {

    /** Additional RFC 9457 extension members, flattened alongside the standard members. */
    val additionalMembers: Map<String, Any?> = Collections.unmodifiableMap(LinkedHashMap(additionalMembers))

    init {
        require(this.additionalMembers.keys.none { it in STANDARD_MEMBERS }) {
            "additionalMembers cannot replace standard problem members: ${this.additionalMembers.keys.intersect(STANDARD_MEMBERS)}"
        }
    }

    internal fun toJsonObject(): Map<String, Any?> = linkedMapOf<String, Any?>(
        "type" to type.toASCIIString(),
        "title" to title,
    ).apply {
        status?.let { put("status", it) }
        detail?.let { put("detail", it) }
        instance?.let { put("instance", it.toASCIIString()) }
        putAll(additionalMembers)
    }

    private companion object {
        val STANDARD_MEMBERS = setOf("type", "title", "status", "detail", "instance")
    }
}
