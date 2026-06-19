/*
 * Javalin - https://javalin.io
 * Copyright 2024 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.mdns

import java.net.InetAddress

/** Configuration for the [MdnsPlugin]. */
class MdnsConfig {
    /** The mDNS hostname to publish (REQUIRED). e.g. `"mycustomstring"` publishes `mycustomstring.local`. */
    @JvmField
    var hostname: String = ""

    /** Whether to also register an `_http._tcp` service record (default true). */
    @JvmField
    var registerHttpService: Boolean = true

    /** The service type to advertise (default `_http._tcp.local.`). */
    @JvmField
    var serviceType: String = "_http._tcp.local."

    /** The service name to advertise. Defaults to [hostname] when null. */
    @JvmField
    var serviceName: String? = null

    /** The port to advertise. Defaults to the server's bound port when null. */
    @JvmField
    var port: Int? = null

    /** TXT record properties to attach to the service. */
    @JvmField
    var properties: MutableMap<String, String> = mutableMapOf()

    /** The address to bind the mDNS responder to. Defaults to the local host when null. */
    @JvmField
    var address: InetAddress? = null
}
