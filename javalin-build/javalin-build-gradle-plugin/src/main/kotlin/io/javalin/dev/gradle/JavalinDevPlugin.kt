package io.javalin.dev.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

open class JavalinDevExtension {
    var mainClass: String? = null
}

class JavalinDevPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("javalinDev", JavalinDevExtension::class.java)
        project.pluginManager.withPlugin("java-base") {
            project.tasks.register("javalinDev", JavalinDevTask::class.java) { task ->
                task.group = "application"
                task.description = "Starts Javalin in dev mode with automatic recompilation and restart"
            }
        }
    }
}
