/*
 * Javalin - https://javalin.io
 * Copyright 2024 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.mdns

import io.javalin.Javalin
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import javax.jmdns.impl.JmDNSImpl

class MdnsPluginTest {

    private val loopback: InetAddress = InetAddress.getByName("127.0.0.1")

    @Test
    fun `app with MdnsPlugin starts and stops cleanly on ephemeral port`() {
        assertThatCode {
            Javalin.create { config ->
                config.registerPlugin(MdnsPlugin {
                    it.hostname = "javalin-test"
                    it.address = loopback
                })
            }.start(0).stop()
        }.doesNotThrowAnyException()
    }

    @Test
    fun `custom serviceName, serviceType and properties are applied to the registered ServiceInfo`() {
        val plugin = MdnsPlugin {
            it.hostname = "javalin-test"
            it.address = loopback
            it.serviceName = "My Custom Service"
            it.serviceType = "_myapp._tcp.local."
            it.properties = mutableMapOf("path" to "/api", "version" to "1.0")
        }
        val app = Javalin.create { it.registerPlugin(plugin) }.start(0)
        try {
            val service = registeredServiceOf(plugin)
            // JmDNS may fail to start where multicast is unavailable (e.g. some CI runners) - skip rather than fail.
            assumeTrue(service != null, "mDNS service not registered in this environment")
            assertThat(service!!.name).isEqualTo("My Custom Service")
            assertThat(service.type).isEqualTo("_myapp._tcp.local.")
            assertThat(service.port).isEqualTo(app.port())
            assertThat(service.getPropertyString("path")).isEqualTo("/api")
            assertThat(service.getPropertyString("version")).isEqualTo("1.0")
        } finally {
            app.stop()
        }
    }

    @Test
    fun `advertised service is resolvable by a second JmDNS instance on loopback`() {
        val plugin = MdnsPlugin {
            it.hostname = "javalin-discovery"
            it.address = loopback
            it.serviceName = "Discoverable Service"
            it.serviceType = "_http._tcp.local."
        }
        val app = Javalin.create { it.registerPlugin(plugin) }.start(0)
        var probe: JmDNS? = null
        try {
            probe = JmDNS.create(loopback)
            val resolved = probe.getServiceInfo("_http._tcp.local.", "Discoverable Service", 3000)
            // Loopback multicast may be unavailable (e.g. on some CI runners) - skip rather than fail.
            assumeTrue(resolved != null, "mDNS not resolvable on loopback in this environment")
            assertThat(resolved!!.name).isEqualTo("Discoverable Service")
            assertThat(resolved.port).isEqualTo(app.port())
        } finally {
            probe?.close()
            app.stop()
        }
    }

    /** Reads the plugin's privately-held [JmDNS] instance and returns its registered service, if any. */
    private fun registeredServiceOf(plugin: MdnsPlugin): ServiceInfo? {
        val field = MdnsPlugin::class.java.getDeclaredField("jmdns").apply { isAccessible = true }
        val jmdns = field.get(plugin) as? JmDNS ?: return null
        return (jmdns as JmDNSImpl).services.values.firstOrNull()
    }
}
