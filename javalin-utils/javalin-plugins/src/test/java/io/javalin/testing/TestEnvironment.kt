package io.javalin.testing

object TestEnvironment {

    // CI server
    val isCiServer = System.getProperty("RunningOnCi")?.toBoolean() == true
    val isNotCiServer = !isCiServer

    // Operating systems
    val os: String = System.getProperty("os.name").lowercase()
    val isMac = os.contains("mac", "darwin")
    val isNotMac = !isMac
    val isWindows = os.contains("win")
    val isNotWindows = !isWindows
    val isLinux = os.contains("nix", "nux", "aix")
    val isNotLinux = !isLinux

    private fun String.contains(vararg strings: String) = strings.any { this.contains(it, ignoreCase = true) }

}
