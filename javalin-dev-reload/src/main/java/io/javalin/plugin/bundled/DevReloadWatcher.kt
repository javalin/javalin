package io.javalin.plugin.bundled

import java.nio.file.Files
import java.nio.file.Path

/**
 * Polls the filesystem for changes by comparing file timestamps.
 * Used by [DevReloadPlugin] to detect source or class file changes.
 *
 * Rate-limited: [checkForChanges] skips the filesystem walk if called again
 * within [cooldownMs] of the last check (default 500ms). This avoids
 * walking the directory tree on every HTTP request.
 */
internal class DevReloadWatcher(
    private val watchPaths: List<Path>,
    private val cooldownMs: Long = 500
) {

    private var baseline: Map<Path, Long> = snapshotTimestamps()
    private var lastCheckTime: Long = System.currentTimeMillis()

    /**
     * Checks for file changes in a single atomic operation.
     * Returns the list of changed files (empty if none or if still within cooldown).
     * Updates the baseline immediately so the same changes aren't reported twice.
     */
    fun checkForChanges(): List<Path> {
        val now = System.currentTimeMillis()
        if (now - lastCheckTime < cooldownMs) return emptyList()
        lastCheckTime = now

        val current = snapshotTimestamps()
        val changes = findChangedFiles(baseline, current)
        if (changes.isNotEmpty()) {
            baseline = current
        }
        return changes
    }

    /** Re-snapshots and replaces the baseline. Used after compilation produces new .class files. */
    fun resetBaseline() {
        baseline = snapshotTimestamps()
        lastCheckTime = System.currentTimeMillis()
    }

    /** Walks all watched directories and records lastModified for every file. */
    private fun snapshotTimestamps(): Map<Path, Long> {
        val result = HashMap<Path, Long>()
        for (root in watchPaths) {
            try {
                Files.walk(root).use { stream ->
                    stream.filter { Files.isRegularFile(it) }.forEach { file ->
                        try {
                            result[file] = Files.getLastModifiedTime(file).toMillis()
                        } catch (_: Exception) {} // file may have been deleted between walk and read
                    }
                }
            } catch (_: Exception) {} // directory may have been deleted
        }
        return result
    }

    /** Returns paths that are new, deleted, or have a different timestamp. */
    private fun findChangedFiles(old: Map<Path, Long>, current: Map<Path, Long>): List<Path> {
        val changed = mutableListOf<Path>()
        for ((path, time) in current) {
            val oldTime = old[path]
            if (oldTime == null || oldTime != time) changed.add(path)
        }
        for (path in old.keys) {
            if (path !in current) changed.add(path) // deleted
        }
        return changed
    }
}
