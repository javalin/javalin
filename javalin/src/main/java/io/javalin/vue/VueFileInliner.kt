package io.javalin.vue

import io.javalin.util.javalinLazy
import java.nio.file.Path
import java.util.regex.Matcher

internal object VueFileInliner {
    private val newlineRegex = Regex("\\r?\\n")
    private val unconditionalRegex = Regex("""@inlineFile\(".*"\)""")
    private val devRegex = Regex("""@inlineFileDev\(".*"\)""")
    private val notDevRegex = Regex("""@inlineFileNotDev\(".*"\)""")

    fun String.inlineFiles(isDev: Boolean, nonVueFiles: List<Path>): String {
        val pathMap = nonVueFiles.associateBy { """"/vue/${it.toString().replace("\\", "/").substringAfter("/vue/")}"""" } // normalize keys
        return this.split(newlineRegex).joinToString("\n") { line ->
            if (!line.contains("@inlineFile")) return@joinToString line // nothing to inline
            val matchingKey = pathMap.keys.find { line.contains(it) } ?: throw IllegalStateException("Invalid path found: $line")
            val matchingFileContent by javalinLazy { Matcher.quoteReplacement(pathMap[matchingKey]!!.readText()) }
            when {
                devRegex.containsMatchIn(line) -> if (isDev) line.replace(devRegex, matchingFileContent) else ""
                notDevRegex.containsMatchIn(line) -> if (!isDev) line.replace(notDevRegex, matchingFileContent) else ""
                else -> line.replace(unconditionalRegex, matchingFileContent)
            }
        }
    }
}
