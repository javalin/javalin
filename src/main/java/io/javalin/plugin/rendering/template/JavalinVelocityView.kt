/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.rendering.template

/**
 * This templating renderer uses Velocity Tools' VelocityView class to merge Velocity templates.
 *
 * It let you enrich your template context with constants and <i>scoped</i> toolboxes
 * comprised of a set of key/classname pairs in each scope (requestion, session, application).
 *
 * The class names are <i>tools</i> class names, a tool being any Java/Kotlin object
 * with a default constructor.
 *
 * Tools can be given properties at toolbox configuration time, which will take
 * effect after the tools are instanciated either via standard public setters or
 * via an optional <code>configure(Map)</code> method. Both methods include the
 * following standard properties :
 *
 * request, velocityContext, log, response, session, servletContext, scope, key
 *
 * The scoped toolboxes can be built from code using method shortcuts:
 *
 * JavalinVelocityView.data("foo", "bar).session().tool("something", { MyTool() })
 *
 * @see <a href="http://velocity.apache.org/tools/3.0/view.html">Velocity Tools View</a>
 * @author Claude Brisson
 */

import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import io.javalin.http.Context
import io.javalin.plugin.rendering.FileRenderer
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.tools.ToolInfo
import org.apache.velocity.tools.ToolboxFactory
import org.apache.velocity.tools.config.*
import org.apache.velocity.tools.view.JeeConfig
import org.apache.velocity.tools.view.VelocityView
import java.io.StringWriter
import java.lang.IllegalStateException
import java.nio.charset.StandardCharsets
import javax.servlet.ServletContext

object JavalinVelocityView : FileRenderer {

    private var velocityView: VelocityView? = null
    private var velocityEngine: VelocityEngine? = null

    @JvmStatic
    fun configure(staticVelocityEngine: VelocityEngine) {
        if (velocityView != null) {
            throw IllegalStateException("use configure(VelocityView) or configure(VelocityEngine) but not both")
        }
        velocityEngine = staticVelocityEngine
    }

    @JvmStatic
    fun configure(staticVelocityView: VelocityView) {
        if (velocityView != null) {
            throw IllegalStateException("use configure(VelocityView) or configure(VelocityEngine) but not both")
        }
        velocityView = staticVelocityView
    }

    @JvmStatic
    fun reset() {
        // reset configuration, mainly for tests
        velocityEngine = null
        velocityView = null
        VelocityViewFactory.defaultVelocityView = null
    }

    override fun render(filePath: String, model: Map<String, Any?>, ctx: Context): String {
        System.out.println("@@@@@@@@ JVV " + this + ", DVV " + VelocityViewFactory.defaultVelocityView)
        Util.ensureDependencyPresent(OptionalDependency.VELOCITY)
        val view = velocityView ?: VelocityViewFactory.velocityView(ctx)
        val writer = StringWriter()
        val context = view.createContext(ctx.req, ctx.res)
        for ((key, obj) in model) context.put(key, obj)
        view.getTemplate(filePath, StandardCharsets.UTF_8.name()).merge(context, writer)
        return writer.toString()
    }

    private object VelocityViewFactory
    {
        @Volatile
        var defaultVelocityView : VelocityView? = null

        @JvmStatic
        fun velocityView(ctx: Context) : VelocityView {
            if (velocityEngine != null) {
                return synchronized(this) {
                    if (velocityView != null) {
                        velocityView!!
                    } else {
                        val created = object : VelocityView(ctx.req.servletContext) {
                            // use custom engine
                            override fun init(config: JeeConfig) {
                                velocityEngine = JavalinVelocityView.velocityEngine ?: VelocityEngine()
                                super.init(config)
                            }
                            override fun configure(config: JeeConfig?, velocity: VelocityEngine?) {}
                        }
                        velocityView = created
                        created
                    }
                }
            }
            if (defaultVelocityView != null) {
                return defaultVelocityView!!
            }
            return synchronized(this) {

                if (defaultVelocityView != null) {
                    defaultVelocityView!!
                } else {
                    val created = DefaultVelocityView(ctx.req.servletContext)
                    defaultVelocityView = created
                    created
                }
            }
        }

        private class DefaultVelocityView(servletContext : ServletContext) : VelocityView(servletContext) {

            // tweak Velocity engine config
            override fun configure(config: JeeConfig, velocity: VelocityEngine)
            {
                // first get the default properties from the classpath, and bail if we don't find them
                val defaultProperties = getProperties(DEFAULT_PROPERTIES_PATH, true)
                velocity.setProperties(defaultProperties)

                // stuck in our own defaults
                velocity.setProperty("resource.loaders", "class")
                velocity.setProperty("resource.loader.class.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader")

                initLog()
            }

            // tweak VelocityView config
            override fun configure(config: JeeConfig, factory: ToolboxFactory) {

                val factoryConfig = FactoryConfiguration("VelocityView.configure(config,factory)");

                // add default tools
                getLog().trace("Loading default tools configuration...")
                factoryConfig.addConfiguration(ConfigurationUtils.getDefaultTools())

                // inject our own toolboxes configuration factory
                factoryConfig.addConfiguration(configuration)

                // apply configuration to factory
                getLog().debug("Configuring factory with: {}", factoryConfig)
                configure(factoryConfig)
            }
        }
    }

    private val configuration: EasyFactoryConfiguration by lazy { EasyFactoryConfiguration() }

    /**
     * add a new data
     */
    @JvmStatic
    fun data(key: String, value: Any, type : String = "string") : JavalinVelocityView {
        configuration.data(key, type, value)
        return this
    }

    /**
     * get application toolbox
     */
    @JvmStatic
    fun global() : Toolbox = Toolbox("application")

    /**
     * get session toolbox
     */
    @JvmStatic
    fun session() : Toolbox = Toolbox("session")

    /**
     * get request toolbox
     */
    @JvmStatic
    fun request() : Toolbox = Toolbox("request")

    /**
     * Helper class to create scoped tools
     */
    class Toolbox(scope : String) {

        private val toolbox: ToolboxConfiguration
            = configuration.getToolbox(scope)
            ?: configuration.toolbox(scope).configuration

        /**
         * Get tool configuration for given key
         */
        fun tool(key : String) : ToolConfiguration = toolbox.getTool(key)

        /**
         * Add a new tool by java classname
         *
         * At instantiation, the given properties, if any, will be applied, as long as the following ones:
         * request, velocityContext, log, response, session, servletContext, scope, key
         */
        fun tool(key : String, classname : String, properties: Map<String, Any> = HashMap()) : Toolbox {
            val tool = ToolConfiguration().apply {
                this.key = key
                this.classname = classname
                this.propertyMap = properties
            }
            toolbox.addTool(tool)
            return this
        }

        /**
         * Add a new tool with a kotlin lambda as factory
         *
         * At instantiation, the given properties, if any, will be applied, as long as the following ones:
         * request, velocityContext, log, response, session, servletContext, scope, key
         */
        fun <T : Any> tool(key : String, factory : () -> T, properties: Map<String, Any> = HashMap()) : Toolbox {
            var tool : ToolConfiguration = object : ToolConfiguration() {
                override fun createInfo() : ToolInfo {
                    val info = object : ToolInfo(key,  Any::class.java) {
                        override fun newInstance(): Any = factory.invoke()
                    }
                    info.restrictTo(restrictTo)
                    info.setSkipSetters(skipSetters ?: false)
                    info.addProperties(properties)
                    return info
                }

            }.apply {
                this.key = key
                // this.classname = factory.javaClass.enclosingMethod.returnType.name // not working, returning "void"
                this.classname = "java.lang.Object" // won't be used to instantiate the tool
                this.propertyMap = properties
            }
            toolbox.addTool(tool)
            return this
        }
    }
}
