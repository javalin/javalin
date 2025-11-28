package io.javalin.community.ssl

import io.javalin.community.ssl.util.ConnectorFactory
import io.javalin.community.ssl.util.SSLUtils
import io.javalin.config.JavalinState
import io.javalin.plugin.Plugin
import nl.altindag.ssl.SSLFactory
import nl.altindag.ssl.util.SSLFactoryUtils
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.SecuredRedirectHandler
import org.eclipse.jetty.util.ssl.SslContextFactory
import java.util.function.BiFunction
import java.util.function.Consumer

/**
 * Plugin to add SSL support to Javalin.
 * The configuration is done via the Consumer<SslConfig> passed to the constructor.
 * The plugin will add the connectors to the server and apply the necessary handlers.
 *
 * If you want to reload the SSLContextFactory, you can call the reload method, by keeping a reference to the plugin instance.
 */
class SslPlugin (userConfig: Consumer<SslConfig>) : Plugin<SslConfig>(userConfig,SslConfig()) {

    private var sslFactory: SSLFactory? = null

    override fun onStart(state: JavalinState) {
        //Add the connectors to the server
        createConnectors(pluginConfig).forEach(state.jetty::addConnector)

        if(pluginConfig.redirect && pluginConfig.secure) {
            state.jetty.modifyServer{
                it.handler = SecuredRedirectHandler()
            }
        }

    }

    override fun name(): String = "SSL Plugin"

    /**
     * Reload the SSL configuration with the new certificates and/or keys.
     * @param newConfig The new configuration.
     */
    fun reload(newConfig: Consumer<SslConfig>) {
        val conf = SslConfig()
        newConfig.accept(conf)
        checkNotNull(sslFactory) { "Cannot reload before the plugin has been applied to a Javalin instance, a server has been patched or if the ssl connector is disabled." }
        val newFactory = SSLUtils.getSslFactory(conf, true)
        SSLFactoryUtils.reload(sslFactory, newFactory)
    }


    private fun createConnectors(config: SslConfig): List<BiFunction<Server, HttpConfiguration, Connector>> {

        val sslContextFactory: SslContextFactory.Server?
        if (config.secure) {
            sslFactory = SSLUtils.getSslFactory(config)
            sslContextFactory = SSLUtils.createSslContextFactory(sslFactory)
        } else {
            sslContextFactory = null
        }
        val connectorList = ArrayList<BiFunction<Server, HttpConfiguration, Connector>>()

        val connectorFactory =
            ConnectorFactory(config, sslContextFactory)

        if (config.insecure) {
            connectorList.add(connectorFactory::createInsecureConnector)
        }
        if (config.secure) {
            connectorList.add(connectorFactory::createSecureConnector)
        }
        return connectorList


    }
}
