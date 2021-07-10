package io.javalin.plugin.openapi.utils

object VersionIssuesUtil {
    val javaVersion = System.getProperty("java.version").split(".")[0].replace(Regex("[^0-9]+"), "").toInt()
    val kotlinVersion = KotlinVersion.CURRENT.minor // let's face it, to JetBrains minor means major
    val hasIssue = javaVersion >= 15 || kotlinVersion >= 5
    val warning = try {
        when {
            javaVersion >= 15 && kotlinVersion >= 5 -> "JDK15 and Kotlin 1.5 break reflection in different ways"
            javaVersion >= 15 -> "JDK 15 has a breaking change to reflection"
            kotlinVersion >= 5 -> "Kotlin 1.5 has a breaking change to reflection"
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}
