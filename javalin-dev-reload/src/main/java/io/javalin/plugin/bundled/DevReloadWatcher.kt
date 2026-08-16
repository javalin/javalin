package io.javalin.plugin.bundled

import java.nio.file.Files
import java.nio.file.Path

/**
 * Polls the filesystem for changes by comparing file timestamps.
 * Rate-limited: [checkForChanges] skips the walk if called within [cooldownMs] of the last check.
 */
internal class DevReloadWatcher(
    private val watchPaths: List<Path>,
    private val cooldownMs: Long = 500
) {

    private var baseline: Map<Path, Long> = snapshotTimestamps()
    private var lastCheckTime: Long = System.currentTimeMillis()

    /** Returns changed files (empty if none or within cooldown). Updates baseline atomically. */
    fun checkForChanges(): List<Path> {
        val now = System.currentTimeMillis()
        if (now - lastCheckTime < cooldownMs) return emptyList()
        lastCheckTime = now
        val current = snapshotTimestamps()
        val changed = mutableListOf<Path>()
        for ((path, time) in current) {
            val oldTime = baseline[path]
            if (oldTime == null || oldTime != time) changed.add(path)
        }
        for (path in baseline.keys) {
            if (path !in current) changed.add(path)
        }
        if (changed.isNotEmpty()) baseline = current
        return changed
    }

    /** Re-snapshots the baseline. Used after compilation produces new .class files. */
    fun resetBaseline() {
        baseline = snapshotTimestamps()
        lastCheckTime = System.currentTimeMillis()
    }

    private fun snapshotTimestamps(): Map<Path, Long> {
        val result = HashMap<Path, Long>()
        for (root in watchPaths) {
            try {
                Files.walk(root).use { stream ->
                    stream.filter { Files.isRegularFile(it) }.forEach { file ->
                        try { result[file] = Files.getLastModifiedTime(file).toMillis() }
                        catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }
        return result
    }
}
