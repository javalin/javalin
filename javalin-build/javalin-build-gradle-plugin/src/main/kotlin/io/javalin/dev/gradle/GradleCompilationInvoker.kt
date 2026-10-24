package io.javalin.dev.gradle

import io.javalin.dev.compilation.CompilationInvoker
import io.javalin.dev.compilation.CompilationResult
import io.javalin.dev.log.JavalinDevLogger
import org.gradle.tooling.GradleConnector
import java.io.File

class GradleCompilationInvoker(
    private val rootDir: File,
    private val taskPath: String,
    private val logger: JavalinDevLogger
) : CompilationInvoker {

    init {
        logger.debug("GradleCompilationInvoker created for root directory: ${rootDir.absolutePath}, task: $taskPath")
    }

    override fun invoke(): CompilationResult {
        logger.info("Running Gradle task: $taskPath in ${rootDir.absolutePath}")
        val connector = GradleConnector.newConnector()
            .forProjectDirectory(rootDir)
        val connection = connector.connect()
        return try {
            connection.newBuild()
                .forTasks(taskPath)
                .setStandardOutput(System.out)
                .setStandardError(System.err)
                .run()
            logger.info("Gradle compilation succeeded")
            CompilationResult.SUCCESS
        } catch (e: Exception) {
            logger.error("Gradle compilation failed: ${e.message}")
            CompilationResult.ERROR
        } finally {
            connection.close()
        }
    }
}
