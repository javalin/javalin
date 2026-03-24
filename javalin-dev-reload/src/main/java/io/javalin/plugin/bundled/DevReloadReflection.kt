package io.javalin.plugin.bundled

import io.javalin.config.JavalinConfig
import io.javalin.config.JavalinState
import io.javalin.http.ExceptionHandler
import io.javalin.http.HttpResponseException
import io.javalin.router.InternalRouter
import io.javalin.router.exception.HttpResponseExceptionMapper
import io.javalin.util.JavalinLogger
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer

/**
 * Reflection-based helpers for [DevReloadPlugin]: route clearing, plugin guard,
 * classloader-based lambda reloading, and config capture.
 *
 * All accessed fields exist on stable internal classes and are covered by DevReloadPlugin tests.
 */
@Suppress("UNCHECKED_CAST")
internal object DevReloadReflection {

    // --- Route clearing ---

    /** Clears all HTTP routes, error/exception handlers, and WebSocket handlers. */
    fun resetAllRoutes(state: JavalinState) {
        val router = state.internalRouter
        // HTTP
        clearMapValues(router, InternalRouter::class.java, "httpPathMatcher", "handlerEntries")
        clearCollection(router, InternalRouter::class.java, "httpErrorMapper", "errorHandlers")
        resetExceptionHandlers(router)
        // WebSocket
        val wsRouter = getField(router, InternalRouter::class.java, "wsRouter")
        val wsPathMatcher = wsRouter.javaClass.getMethod("getWsPathMatcher").invoke(wsRouter)
        (getField(wsPathMatcher, wsPathMatcher.javaClass, "wsHandlerEntries") as MutableMap<*, MutableList<*>>).values.forEach { it.clear() }
        val wsExMapper = wsRouter.javaClass.getMethod("getWsExceptionMapper").invoke(wsRouter)
        (wsExMapper.javaClass.getMethod("getHandlers").invoke(wsExMapper) as MutableMap<*, *>).clear()
    }

    private fun resetExceptionHandlers(router: InternalRouter) {
        val mapper = getField(router, InternalRouter::class.java, "httpExceptionMapper")
        val handlers = mapper.javaClass.getMethod("getHandlers").invoke(mapper) as MutableMap<Class<out Exception>, ExceptionHandler<Exception>?>
        handlers.clear()
        handlers[HttpResponseException::class.java] = ExceptionHandler { e, ctx ->
            HttpResponseExceptionMapper.handle(e as HttpResponseException, ctx)
        }
    }

    private fun clearMapValues(parent: Any, parentClass: Class<*>, fieldName: String, nestedField: String) {
        val obj = getField(parent, parentClass, fieldName)
        (getField(obj, obj.javaClass, nestedField) as MutableMap<*, MutableList<*>>).values.forEach { it.clear() }
    }

    private fun clearCollection(parent: Any, parentClass: Class<*>, fieldName: String, nestedField: String) {
        val obj = getField(parent, parentClass, fieldName)
        (getField(obj, obj.javaClass, nestedField) as MutableCollection<*>).clear()
    }

    // --- Plugin guard ---

    /**
     * Temporarily suppresses duplicate plugin registration during config consumer re-execution.
     * Swaps PluginManager's internal lists with empty guarded wrappers that silently ignore
     * add() for already-registered plugin types, then restores originals afterward.
     */
    fun <T> withPluginReloadingEnabled(state: JavalinState, block: () -> T): T {
        val pm = state.pluginManager
        val clazz = pm.javaClass
        val fields = listOf("plugins", "initializedPlugins", "enabledPlugins")
            .map { clazz.getDeclaredField(it).apply { isAccessible = true } }
        val originals = fields.map { it.get(pm) as MutableList<Any> }
        val existingClasses = originals[0].map { it.javaClass }.toSet()

        fun guardedList() = object : ArrayList<Any>() {
            override fun add(element: Any) = if (element.javaClass in existingClasses) true else super.add(element)
        }
        fields.forEach { it.set(pm, guardedList()) }
        try {
            return block()
        } finally {
            fields.zip(originals).forEach { (field, orig) -> field.set(pm, orig) }
        }
    }

    // --- Config capture & classloader reload ---

    /** Creates a JavalinConfig via reflection (its constructor is internal to the javalin module). */
    fun createJavalinConfig(state: JavalinState): JavalinConfig {
        val ctor = JavalinConfig::class.java.declaredConstructors.first()
        ctor.isAccessible = true
        return ctor.newInstance(state) as JavalinConfig
    }

    /**
     * Builds a config consumer by locating the original lambda method via the class/method
     * names captured at construction time. Returns null if not found.
     */
    fun captureUserConfig(lambdaInfo: Pair<String, String>?, log: (String) -> Unit): Consumer<JavalinConfig>? {
        val (methodName, className) = lambdaInfo ?: return null
        try {
            val method = Class.forName(className).declaredMethods.find { it.name == methodName } ?: return null
            method.isAccessible = true
            log("DevReloadPlugin: Captured config lambda '$methodName' from $className")
            return Consumer { config -> method.invoke(null, config) }
        } catch (e: Exception) {
            log("DevReloadPlugin: Could not capture config lambda: ${e.message}")
            return null
        }
    }

    /**
     * Creates a fresh child-first classloader, reloads the enclosing class from disk,
     * finds the config lambda method, and returns a consumer that invokes it.
     * Falls back to [originalConsumer] on any failure.
     */
    fun reloadConsumerFromDisk(
        originalConsumer: Consumer<JavalinConfig>,
        lambdaInfo: Pair<String, String>?,
        classOutputDirs: List<Path>,
        previousClassLoader: URLClassLoader?,
        log: (String) -> Unit
    ): Pair<Consumer<JavalinConfig>, URLClassLoader?> {
        try {
            val classpathUrls = classOutputDirs
                .map { it.toAbsolutePath().normalize() }
                .filter { Files.isDirectory(it) }
                .map { it.toUri().toURL() }
                .toTypedArray()
            if (classpathUrls.isEmpty()) return originalConsumer to previousClassLoader

            val enclosingClassName = lambdaInfo?.second ?: run {
                val name = originalConsumer.javaClass.name
                if ("$$" in name) name.substringBefore("$$") else name
            }
            log("DevReloadPlugin: Reloading $enclosingClassName via fresh classloader")

            try { previousClassLoader?.close() } catch (_: Exception) {}

            val freshCL = childFirstClassLoader(classpathUrls, enclosingClassName)
            val reloadedClass = freshCL.loadClass(enclosingClassName)

            val targetMethodName = lambdaInfo?.first ?: resolveViaSerializedLambda(originalConsumer, log)

            val configLambdas = reloadedClass.declaredMethods.filter { m ->
                java.lang.reflect.Modifier.isStatic(m.modifiers)
                    && m.name.contains("lambda")
                    && m.parameterCount == 1
                    && m.parameterTypes[0].name == JavalinConfig::class.java.name
            }
            val target = if (targetMethodName != null) configLambdas.find { it.name == targetMethodName }
                else configLambdas.singleOrNull() ?: configLambdas.firstOrNull()

            if (target != null) {
                target.isAccessible = true
                log("DevReloadPlugin: Using lambda '${target.name}' from $enclosingClassName")
                return Consumer<JavalinConfig> { config -> target.invoke(null, config) } to freshCL
            }
            log("DevReloadPlugin: No config lambda found in $enclosingClassName, using original consumer")
            return originalConsumer to freshCL
        } catch (e: Exception) {
            JavalinLogger.warn("DevReloadPlugin: Classloader reload failed, using original consumer", e)
            return originalConsumer to previousClassLoader
        }
    }

    private fun resolveViaSerializedLambda(consumer: Consumer<JavalinConfig>, log: (String) -> Unit): String? {
        return try {
            val writeReplace = consumer.javaClass.getDeclaredMethod("writeReplace")
            writeReplace.isAccessible = true
            val name = (writeReplace.invoke(consumer) as java.lang.invoke.SerializedLambda).implMethodName
            log("DevReloadPlugin: Found target method '$name' via SerializedLambda")
            name
        } catch (_: Exception) { null }
    }

    /** Child-first classloader: loads app classes from disk, delegates framework classes to parent. */
    private fun childFirstClassLoader(urls: Array<java.net.URL>, enclosingClassName: String): URLClassLoader {
        return object : URLClassLoader(urls, DevReloadReflection::class.java.classLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                synchronized(getClassLoadingLock(name)) {
                    var c = findLoadedClass(name)
                    if (c == null) {
                        val isEnclosing = name == enclosingClassName || name.startsWith("$enclosingClassName$")
                        val delegate = !isEnclosing && (
                            name.startsWith("io.javalin.") || name.startsWith("java.") ||
                            name.startsWith("javax.") || name.startsWith("jdk.") ||
                            name.startsWith("sun.") || name.startsWith("jakarta.") ||
                            name.startsWith("kotlin.") || name.startsWith("org.eclipse.jetty.") ||
                            name.startsWith("org.slf4j."))
                        c = if (delegate) parent.loadClass(name)
                            else try { findClass(name) } catch (_: ClassNotFoundException) { parent.loadClass(name) }
                    }
                    if (resolve) resolveClass(c)
                    return c
                }
            }
        }
    }

    private fun getField(target: Any, clazz: Class<*>, fieldName: String): Any {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(target)
    }
}
