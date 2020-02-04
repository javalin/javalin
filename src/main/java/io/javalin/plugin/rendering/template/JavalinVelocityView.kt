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
 * via an optional <code>configure(Map)</code> method.
 *
 * The scoped toolboxes can be built from code using the JavalinVelocityView.data()
 * and JavalinVelocityView.tool() methods.
 *
 * @see <a href="http://velocity.apache.org/tools/3.0/view.html">Velocity Tools View</a>
 * @author Claude Brisson
 */

import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import io.javalin.http.Context
import io.javalin.plugin.rendering.FileRenderer
import org.apache.velocity.tools.config.EasyFactoryConfiguration
import org.apache.velocity.tools.config.ToolConfiguration
import org.apache.velocity.tools.config.ToolboxConfiguration
import org.apache.velocity.tools.view.ServletUtils.CONFIGURATION_KEY
import org.apache.velocity.tools.view.VelocityView
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.util.*
import javax.servlet.ServletContext
import kotlin.collections.HashMap

object JavalinVelocityView : FileRenderer {

    private var velocityView: VelocityView? = null

    @JvmStatic
    fun configure(staticVelocityView: VelocityView) {
        velocityView = staticVelocityView
    }

    override fun render(filePath: String, model: Map<String, Any?>, ctx: Context): String {
        Util.ensureDependencyPresent(OptionalDependency.VELOCITY)
        val view = velocityView ?: DefaultVelocityViewFactory.velocityView(ctx)
        val writer = StringWriter()
        view.getTemplate(filePath, StandardCharsets.UTF_8.name()).merge(
                view.createContext(ctx.req, ctx.res), writer)
        return writer.toString()
    }

    // the 'by lazy' construct used in other rendering plugins cannot be used here,
    // because VelocityView constructor needs a ServletContext object
    private object DefaultVelocityViewFactory
    {
        private var defaultVelocityView : VelocityView? = null
        @JvmStatic
        fun velocityView(ctx: Context) : VelocityView {
            val view = defaultVelocityView;
            if (view != null) {
                return view;
            }
            return synchronized(this) {

                val view2 = defaultVelocityView
                if (view2 != null) { view2 }
                else {
                    val created = DefaultVelocityView(ctx.req.servletContext)
                    defaultVelocityView = created
                    created
                }
            }
        }
    }

    public class DefaultVelocityView (servletContext : ServletContext) : VelocityView(servletContext) {

        init {
            // inject our own toolboxes configuration factory
            servletContext.setAttribute(CONFIGURATION_KEY, configuration)
        }

        // tweak Velocity configuration
        override fun getProperties(path: String, required: Boolean): Properties {

            var props = super.getProperties(path, required)

            // stuck in our own defaults after calling super
            if (path == DEFAULT_PROPERTIES_PATH) {
                // override properties using their deprecated Velocity pre-2.1 property names (for Tools 3.0)
                props.setProperty("resource.loader", "class");
                props.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
                // note: next version of Tools (3.1+) will use Velocity 2.1+ property names, hence:
                // props.setProperty("resource.loaders", "class");
                // props.setProperty("resource.loader.class.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            }
            return props;
        }
    }

    private val configuration: EasyFactoryConfiguration by lazy {
        val conf = EasyFactoryConfiguration()
        conf.addDefaultTools() // always add default tools
        conf
    }

    /**
     * add a new data
     */
    @JvmStatic
    fun data(key: String, value: Any, type : String = "string") : JavalinVelocityView {
        configuration.data(key, type, value)
        return this
    }

    /**
     * add a new scoped tool
     */
    @JvmStatic
    fun tool(key: String, classname: String, scope: String = "application", properties: Map<String, Any> = HashMap()) : JavalinVelocityView {
        val toolbox: ToolboxConfiguration
                = configuration.getToolbox(scope)
                ?: configuration.toolbox(scope).configuration
        val tool = ToolConfiguration()
        tool.key = key
        tool.classname = classname
        tool.propertyMap = properties
        toolbox.addTool(tool)
        return this
    }
}
