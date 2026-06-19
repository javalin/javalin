/*
 * Javalin - https://javalin.io
 * Copyright 2024 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.mdns

import io.javalin.config.JavalinState
import io.javalin.plugin.Plugin
import org.eclipse.jetty.server.ServerConnector
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.util.function.Consumer
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

/**
 * Publishes a custom mDNS hostname for a running Javalin server, so a chosen name like
 * `mycustomstring.local` resolves to the machine's IP on the local network. Optionally also
 * registers an `_http._tcp` service record so the app is browseable in Bonjour/Zeroconf tools.
 *
 * mDNS is link-local multicast: it works only on the same LAN and does not route across
 * subnets or the public internet. Use it for same-LAN zero-config discovery; use real DNS or
 * platform service discovery elsewhere. See the module README for when (not) to use it.
 *
 * Example usage:
 * ```kotlin
 * Javalin.create { config ->
 *     config.registerPlugin(MdnsPlugin {
 *         it.hostname = "mycustomstring"
 *     })
 * }
 * ```
 */
class MdnsPlugin @JvmOverloads constructor(
    userConfig: Consumer<MdnsConfig>? = null
) : Plugin<MdnsConfig>(userConfig, MdnsConfig()) {

    private var jmdns: JmDNS? = null

    override fun onInitialize(state: JavalinState) {
        require(pluginConfig.hostname.isNotBlank()) {
            "MdnsPlugin requires a non-blank 'hostname' (e.g. \"mycustomstring\" to publish mycustomstring.local)"
        }
    }

    override fun onStart(state: JavalinState) {
        state.events.serverStarted { start(state) }
        state.events.serverStopping { stop() }
    }

    private fun start(state: JavalinState) {
        try {
            val address = pluginConfig.address ?: InetAddress.getLocalHost()
            val jmdns = JmDNS.create(/*addr=*/ address, /*name=*/ pluginConfig.hostname).also { this.jmdns = it }
            logger.info("mDNS hostname published: {}.local -> {}", pluginConfig.hostname, address.hostAddress)

            if (pluginConfig.registerHttpService) {
                val port = pluginConfig.port ?: boundPort(state)
                val serviceName = pluginConfig.serviceName ?: pluginConfig.hostname
                val service = ServiceInfo.create(
                    /*type=*/ pluginConfig.serviceType,
                    /*name=*/ serviceName,
                    /*port=*/ port,
                    /*weight=*/ 0,
                    /*priority=*/ 0,
                    /*props=*/ pluginConfig.properties
                )
                jmdns.registerService(service)
                logger.info("mDNS service registered: {} ({}) on port {}", serviceName, pluginConfig.serviceType, port)
            }
        } catch (e: Exception) {
            logger.warn("Failed to start mDNS responder", e)
        }
    }

    private fun stop() {
        try {
            jmdns?.let {
                it.unregisterAllServices()
                it.close()
            }
        } catch (e: Exception) {
            logger.warn("Failed to stop mDNS responder", e)
        } finally {
            jmdns = null
        }
    }

    private fun boundPort(state: JavalinState): Int =
        (state.jettyInternal.server?.connectors?.firstOrNull() as? ServerConnector)?.localPort
            ?: state.jetty.port

    companion object {
        private val logger = LoggerFactory.getLogger(MdnsPlugin::class.java)
    }
}
