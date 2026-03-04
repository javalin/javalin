package io.javalin.plugin.bundled

import java.nio.file.Files
import java.nio.file.Path

/**
 * Polls the filesystem for changes by comparing file timestamps.
 * Used by [DevReloadPlugin] to detect source or class file changes.
 */
internal class DevReloadWatcher(private val watchPaths: List<Path>) {

    private var timestamps: Map<Path, Long> = snapshotTimestamps()

    /** Takes a fresh snapshot, replacing the current baseline. */
    fun resetBaseline() {
        timestamps = snapshotTimestamps()
    }

    /**
     * Compares the current filesystem state against the baseline.
     * Returns changed files (new, modified, or deleted) — empty list means no changes.
     * Does NOT update the baseline; call [acceptChanges] or [resetBaseline] after handling.
     */
    fun findChanges(): List<Path> {
        val current = snapshotTimestamps()
        return findChangedFiles(timestamps, current)
    }

    /**
     * Re-snapshots and updates the baseline.
     * Returns the changed files compared to the previous baseline.
     */
    fun acceptChanges(): List<Path> {
        val current = snapshotTimestamps()
        val changes = findChangedFiles(timestamps, current)
        timestamps = current
        return changes
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

