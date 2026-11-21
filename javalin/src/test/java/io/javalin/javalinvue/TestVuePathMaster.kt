package io.javalin.javalinvue

import io.javalin.Javalin
import io.javalin.plugin.bundled.JavalinVuePlugin
import io.javalin.testing.TestUtil
import io.javalin.vue.VueComponent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class VueResourceMarker

class TestVuePathMaster {
    @TempDir
    lateinit var temp: Path

    private val markerName = VueResourceMarker::class.java.name
    private val markerEntry = markerName.replace('.', '/') + ".class"
    private val markerBytes = VueResourceMarker::class.java.classLoader.getResourceAsStream(markerEntry)!!.use { it.readBytes() }

    @Test
    fun `loads Vue resources without class package directory entries`() {
        val jar = writeJar("no-class-directories.jar", marker = true, directories = false, resources = true)
        URLClassLoader(arrayOf(jar.toUri().toURL()), null).use { loader ->
            val marker = loader.loadClass(markerName)
            assertThat(marker.getResource("")).isNull()
            assertThat(marker.getResource("/vue")).isNotNull()
            assertLayout(loader)
        }
    }

    @Test
    fun `loads Vue resources from a different jar than the marker class`() {
        val library = writeJar("library.jar", marker = true, directories = true, resources = false)
        val resources = writeJar("resources.jar", marker = false, directories = false, resources = true)
        URLClassLoader(arrayOf(library.toUri().toURL(), resources.toUri().toURL()), null).use { assertLayout(it) }
    }

    @Test
    fun `loads Vue resources from a jar with class package directory entries`() {
        val jar = writeJar("with-directories.jar", marker = true, directories = true, resources = true)
        URLClassLoader(arrayOf(jar.toUri().toURL()), null).use { assertLayout(it) }
    }

    @Test
    fun `loads exploded Vue resources`() {
        Files.createDirectories(temp.resolve(markerEntry).parent)
        Files.write(temp.resolve(markerEntry), markerBytes)
        Files.createDirectories(temp.resolve("vue"))
        Files.writeString(temp.resolve("vue/layout.html"), layout)
        Files.writeString(temp.resolve("vue/packaged-component.vue"), component)
        URLClassLoader(arrayOf(temp.toUri().toURL()), null).use { assertLayout(it) }
    }

    private fun assertLayout(loader: URLClassLoader) {
        val marker = loader.loadClass(markerName)
        var root: Path? = null
        var app: Javalin? = null
        try {
            app = Javalin.create { config ->
                config.registerPlugin(JavalinVuePlugin { vue ->
                    vue.rootDirectory("/vue", resourcesJarClass = marker)
                    vue.isDevFunction = { false }
                    root = vue.rootDirectory
                })
                config.routes.get("/", VueComponent("packaged-component"))
            }
            TestUtil.test(app) { _, http ->
                val response = http.get("/")
                assertThat(response.status).isEqualTo(200)
                assertThat(response.body).contains("layout-ok", "packaged-content", "<packaged-component></packaged-component>")
            }
        } finally {
            app?.stop()
            root?.takeIf { it.toUri().scheme == "jar" }?.fileSystem?.close()
        }
    }

    private fun writeJar(name: String, marker: Boolean, directories: Boolean, resources: Boolean): Path {
        val target = temp.resolve(name)
        JarOutputStream(Files.newOutputStream(target)).use { jar ->
            fun entry(name: String, bytes: ByteArray = byteArrayOf()) {
                jar.putNextEntry(JarEntry(name))
                jar.write(bytes)
                jar.closeEntry()
            }
            if (marker) {
                if (directories) {
                    val parts = markerEntry.split('/').dropLast(1)
                    parts.indices.forEach { entry(parts.take(it + 1).joinToString("/") + "/") }
                }
                entry(markerEntry, markerBytes)
            }
            if (resources) {
                entry("vue/")
                entry("vue/layout.html", layout.toByteArray())
                entry("vue/packaged-component.vue", component.toByteArray())
            }
        }
        return target
    }

    private val layout = "<html><head>@componentRegistration</head><body>layout-ok @routeComponent</body></html>"
    private val component = """
        <template id="packaged-component"><div>packaged-content</div></template>
        <script>Vue.component("packaged-component", {template: "#packaged-component"});</script>
    """.trimIndent()
}
