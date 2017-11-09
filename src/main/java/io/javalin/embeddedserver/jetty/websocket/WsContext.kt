/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty.websocket

import org.eclipse.jetty.websocket.api.Session

class WsContext(@JvmField val session: Session) {
    @JvmField
    var message: String? = null
    @JvmField
    var throwable: Throwable? = null
    @JvmField
    var statusCode: Int? = null
    @JvmField
    var reason: String? = null

    constructor(session: Session, message: String) : this(session) {
        this.message = message;
    }

    constructor(session: Session, statusCode: Int, reason: String) : this(session) {
        this.statusCode = statusCode;
        this.reason = reason;
    }

    constructor(session: Session, throwable: Throwable) : this(session) {
        this.throwable = throwable;
    }

    fun send(message: String) {
        session.remote.sendString(message)
    }

}
