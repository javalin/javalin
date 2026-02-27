package io.javalin.dev.gradle

import io.javalin.dev.compilation.CompilationInvoker
import io.javalin.dev.compilation.CompilationResult
import io.javalin.dev.compilation.CompilationSourcesTracker
import io.javalin.dev.log.JavalinDevLogger
import io.javalin.dev.main.ApplicationMainClassCandidate
import io.javalin.dev.main.ApplicationMainClassScanner
import io.javalin.dev.main.ApplicationMainClassType
import io.javalin.dev.proxy.ApplicationProxy
import io.javalin.dev.runner.ApplicationRunner
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URL

abstract class JavalinDevTask : DefaultTask() {

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun execute() {
        logger.lifecycle("Starting dev mode...")
        val devLogger: JavalinDevLogger = GradleDevLogger(logger)

        val mainClassOverride = project.extensions.findByType(JavalinDevExtension::class.java)?.mainClass
            ?: project.findProperty("mainClass") as? String

        val taskPath = if (project == project.rootProject) "classes" else "${project.path}:classes"
        val compiler = GradleCompilationInvoker(project.rootProject.projectDir, taskPath, devLogger)

        compileApplication(compiler, devLogger)

        val runner = createApplicationRunner(mainClassOverride, devLogger)
        val instance = runner.start()

        CompilationSourcesTracker(devLogger).use { tracker ->
            registerWatchedSources(tracker, devLogger)
            tracker.recordBaseline()

            val proxyPort = waitForAppInit(devLogger)
            val proxy = ApplicationProxy(tracker, compiler, runner, instance, devLogger)
            proxy.start(proxyPort)
        }
    }

    private fun compileApplication(compiler: CompilationInvoker, devLogger: JavalinDevLogger) {
        devLogger.info("Compiling...")
        val result = compiler.invoke()
        if (result == CompilationResult.ERROR) {
            throw GradleException("Initial compilation failed")
        }
        devLogger.info("Compilation successful")
    }

    private fun createApplicationRunner(mainClassOverride: String?, devLogger: JavalinDevLogger): ApplicationRunner {
        val dependencyUrls = resolveDependencyUrls()
        val classesUrls = resolveClassesUrls()
        val mainClass = resolveMainClass(mainClassOverride, devLogger)
        return ApplicationRunner(dependencyUrls, classesUrls, mainClass, devLogger)
    }

    private fun resolveDependencyUrls(): Array<URL> {
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val mainSourceSet = sourceSets.getByName("main")
        val outputFiles = mainSourceSet.output.files

        return project.configurations.getByName("runtimeClasspath")
            .resolve()
            .filter { it !in outputFiles }
            .map { it.toURI().toURL() }
            .toTypedArray()
    }

    private fun resolveClassesUrls(): Array<URL> {
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val mainSourceSet = sourceSets.getByName("main")
        val urls = mutableListOf<URL>()
        mainSourceSet.output.classesDirs.forEach { urls.add(it.toURI().toURL()) }
        mainSourceSet.output.resourcesDir?.let { urls.add(it.toURI().toURL()) }
        return urls.toTypedArray()
    }

    private fun resolveMainClass(mainClassOverride: String?, devLogger: JavalinDevLogger): ApplicationMainClassCandidate {
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val mainSourceSet = sourceSets.getByName("main")
        val scanner = ApplicationMainClassScanner(devLogger)

        if (!mainClassOverride.isNullOrBlank()) {
            for (classesDir in mainSourceSet.output.classesDirs) {
                if (!classesDir.exists()) continue
                val classFile = classesDir.toPath().resolve(
                    mainClassOverride.replace(".", File.separator) + ".class"
                )
                val result = scanner.scanFile(classFile)
                if (result.isPresent) {
                    return result.get()
                }
            }
            throw GradleException("Cannot find main class: $mainClassOverride")
        }

        // Auto-detect by scanning all classes directories
        for (classesDir in mainSourceSet.output.classesDirs) {
            if (!classesDir.exists()) continue
            val candidates = scanner.scanDirectory(classesDir.toPath())
            for (type in ApplicationMainClassType.values()) {
                val byType: List<ApplicationMainClassCandidate> = candidates[type] ?: continue
                if (byType.isEmpty()) continue
                if (byType.size == 1) return byType[0]

                val names = byType.joinToString(", ") { it.className() }
                throw GradleException(
                    "Multiple main classes found: $names" +
                    ". Specify with -PmainClass=com.example.App"
                )
            }
        }

        throw GradleException(
            "No main classes found" +
            ". Create one or specify with -PmainClass=com.example.App"
        )
    }

    private fun registerWatchedSources(tracker: CompilationSourcesTracker, devLogger: JavalinDevLogger) {
        devLogger.info("Registering watched sources...")

        // Watch settings file
        for (name in listOf("settings.gradle.kts", "settings.gradle")) {
            val file = project.rootProject.file(name)
            if (file.exists()) tracker.watchFile(file.toPath())
        }

        // Watch current project
        watchProjectSources(project, tracker, devLogger)

        // Watch project dependencies (equivalent to Maven reactor dependencies)
        val projectDeps = project.configurations.getByName("runtimeClasspath")
            .allDependencies
            .filterIsInstance<ProjectDependency>()
            .map { project.project(it.path) }

        for (dep in projectDeps) {
            devLogger.info("Watching project dependency: ${dep.path}")
            watchProjectSources(dep, tracker, devLogger)
        }
        devLogger.info("Watched sources registration complete")
    }

    private fun watchProjectSources(proj: Project, tracker: CompilationSourcesTracker, devLogger: JavalinDevLogger) {
        devLogger.debug("Watching project: ${proj.group}:${proj.name}")

        // Build file (build.gradle.kts or build.gradle)
        if (proj.buildFile.exists()) {
            tracker.watchFile(proj.buildFile.toPath())
        }

        // Source and resource directories
        val sourceSets = proj.extensions.findByType(SourceSetContainer::class.java) ?: return
        val mainSourceSet = sourceSets.findByName("main") ?: return

        for (srcDir in mainSourceSet.allSource.srcDirs) {
            tracker.watchDirectory(srcDir.toPath())
        }
    }

    private fun waitForAppInit(devLogger: JavalinDevLogger): Int {
        devLogger.debug("Waiting for application to set javalin.dev.requestedPort...")
        while (!Thread.interrupted()) {
            val value = System.getProperty("javalin.dev.requestedPort")
            if (value != null) {
                val port = value.toInt()
                devLogger.info("Application requested proxy port: $port")
                return port
            }
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
        throw GradleException("Could not detect requested port")
    }
}
