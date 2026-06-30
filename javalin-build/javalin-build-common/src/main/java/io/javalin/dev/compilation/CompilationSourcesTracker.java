package io.javalin.dev.compilation;

import io.javalin.dev.log.JavalinDevLogger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.StandardWatchEventKinds.*;

public class CompilationSourcesTracker implements AutoCloseable {
    private final JavalinDevLogger logger;
    private final WatchService watchService;
    private final Map<WatchKey, Path> keyToDir;
    private final Set<Path> fileOnlyDirs;
    private final Set<Path> watchedFiles;

    private final Object lock;
    private final Set<Path> added;
    private final Set<Path> modified;
    private final Set<Path> deleted;

    private volatile Thread watchThread;
    private volatile boolean closed;

    public CompilationSourcesTracker(JavalinDevLogger logger) throws IOException {
        this.logger = logger;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.keyToDir = new HashMap<>();
        this.fileOnlyDirs = new HashSet<>();
        this.watchedFiles = new HashSet<>();
        this.lock = new Object();
        this.added = new HashSet<>();
        this.modified = new HashSet<>();
        this.deleted = new HashSet<>();
        this.watchThread = null;
        this.closed = false;
        logger.debug("CompilationSourcesTracker initialized with WatchService");
    }

    public void watchDirectory(Path dir) throws IOException {
        if (dir == null || Files.notExists(dir)) {
            logger.debug("Skipping watch for non-existent directory: " + dir);
            return;
        }
        Path normalized = dir.toAbsolutePath().normalize();
        if (Files.isDirectory(normalized)) {
            logger.info("Watching directory recursively: " + normalized);
            registerRecursive(normalized);
        }
    }

    public void watchFile(Path file) throws IOException {
        if (file == null || Files.notExists(file)) {
            logger.debug("Skipping watch for non-existent file: " + file);
            return;
        }
        Path normalized = file.toAbsolutePath().normalize();
        Path parent = normalized.getParent();
        if (parent != null && Files.isDirectory(parent)) {
            watchedFiles.add(normalized);
            registerSingle(parent);
            logger.info("Watching file: " + normalized);
        }
    }

    public synchronized void recordBaseline() {
        // Discard any events generated during registration
        synchronized (lock) {
            added.clear();
            modified.clear();
            deleted.clear();
        }
        logger.debug("Baseline recorded, clearing initial events");
        startWatchThread();
    }

    public CompilationSourcesList getAndClearChanges() {
        synchronized (lock) {
            CompilationSourcesList result = new CompilationSourcesList(
                List.copyOf(added),
                List.copyOf(modified),
                List.copyOf(deleted)
            );

            added.clear();
            modified.clear();
            deleted.clear();

            if (!result.isEmpty()) {
                logger.info("Detected source changes: added: " + result.added().size()
                    + ", modified: " + result.modified().size()
                    + ", deleted: " + result.deleted().size());
                for (Path p : result.added()) {
                    logger.debug("  + " + p);
                }
                for (Path p : result.modified()) {
                    logger.debug("  ~ " + p);
                }
                for (Path p : result.deleted()) {
                    logger.debug("  - " + p);
                }
            } else {
                logger.debug("No source changes detected since last check");
            }

            return result;
        }
    }

    private void registerRecursive(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
                keyToDir.put(key, dir);
                fileOnlyDirs.remove(dir);
                logger.debug("Registered watch on directory: " + dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void registerSingle(Path dir) throws IOException {
        if (keyToDir.containsValue(dir)) {
            return;
        }
        WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        keyToDir.put(key, dir);
        fileOnlyDirs.add(dir);
        logger.debug("Registered single-directory watch on: " + dir);
    }

    private void startWatchThread() {
        if (watchThread != null) {
            return;
        }

        watchThread = new Thread(new WatchEventProcessor());
        watchThread.setDaemon(true);
        watchThread.setName("compilation-source-watcher");
        watchThread.start();
        logger.info("File watcher thread started");
    }

    @Override
    public void close() throws IOException {
        logger.info("Closing CompilationSourcesTracker");
        closed = true;
        watchService.close();
        if (watchThread != null) {
            watchThread.interrupt();
        }
        logger.debug("CompilationSourcesTracker closed, watch thread interrupted");
    }

    private class WatchEventProcessor implements Runnable {

        @Override
        public void run() {
            logger.debug("WatchEventProcessor started, waiting for file system events");
            while (!closed) {
                try {
                    WatchKey key = watchService.take();

                    logger.debug("WatchEventProcessor processing key: " + key);

                    Path dir = keyToDir.get(key);
                    if (dir == null) {
                        key.cancel();
                        logger.debug("Received watch key for unknown directory, cancelling");
                        continue;
                    }

                    processEvents(key, dir);

                    boolean valid = key.reset();
                    if (!valid) {
                        keyToDir.remove(key);
                        logger.warn("Watch key invalidated for directory: " + dir);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.debug("WatchEventProcessor interrupted, shutting down");
                    break;
                } catch (ClosedWatchServiceException e) {
                    logger.debug("WatchService closed, stopping event processor");
                    break;
                }
            }
        }

        private void processEvents(WatchKey key, Path dir) {
            boolean isFileOnly = fileOnlyDirs.contains(dir);

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == OVERFLOW) {
                    logger.warn("File system event overflow detected in: " + dir);
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                Path resolved = dir.resolve(pathEvent.context());

                if (isFileOnly && !watchedFiles.contains(resolved)) {
                    logger.debug("Skipping non-watched file: " + resolved);
                    continue;
                }

                if (kind == ENTRY_CREATE && Files.isDirectory(resolved, LinkOption.NOFOLLOW_LINKS)) {
                    try {
                        logger.debug("New directory detected, registering recursively: " + resolved);
                        registerRecursive(resolved);
                    } catch (IOException e) {
                        logger.warn("Failed to register new directory: " + resolved + " (" + e.getMessage() + ")");
                    }
                }

                logger.debug("File event: " + kind.name() + " -> " + resolved);
                categorize(kind, resolved);
            }
        }

        private void categorize(WatchEvent.Kind<?> kind, Path path) {
            synchronized (lock) {
                if (kind == ENTRY_CREATE) {
                    deleted.remove(path);
                    modified.remove(path);
                    added.add(path);
                } else if (kind == ENTRY_MODIFY) {
                    if (!added.contains(path)) {
                        modified.add(path);
                    }
                } else if (kind == ENTRY_DELETE) {
                    if (!added.remove(path)) {
                        modified.remove(path);
                        deleted.add(path);
                    }
                }
            }
        }
    }
}
