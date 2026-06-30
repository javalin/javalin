package io.javalin.dev.compilation;

import io.javalin.dev.testutil.TestLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class CompilationSourcesTrackerTest {

    @TempDir
    Path tempDir;

    private final TestLogger logger = new TestLogger();
    private CompilationSourcesTracker tracker;

    @AfterEach
    void tearDown() throws Exception {
        if (tracker != null) {
            tracker.close();
        }
    }

    private CompilationSourcesTracker createTracker() throws IOException {
        tracker = new CompilationSourcesTracker(logger);
        return tracker;
    }

    @Test
    void constructor_initializes() throws Exception {
        createTracker();
        assertThat(logger.hasMessage("debug", "CompilationSourcesTracker initialized")).isTrue();
    }

    @Test
    void watchDirectory_null_skips() throws Exception {
        var t = createTracker();
        t.watchDirectory(null);
        assertThat(logger.hasMessage("debug", "Skipping watch for non-existent directory: null")).isTrue();
    }

    @Test
    void watchDirectory_nonExistent_skips() throws Exception {
        var t = createTracker();
        t.watchDirectory(tempDir.resolve("nonexistent"));
        assertThat(logger.hasMessage("debug", "Skipping watch for non-existent directory")).isTrue();
    }

    @Test
    void watchFile_null_skips() throws Exception {
        var t = createTracker();
        t.watchFile(null);
        assertThat(logger.hasMessage("debug", "Skipping watch for non-existent file: null")).isTrue();
    }

    @Test
    void watchFile_nonExistent_skips() throws Exception {
        var t = createTracker();
        t.watchFile(tempDir.resolve("nofile.txt"));
        assertThat(logger.hasMessage("debug", "Skipping watch for non-existent file")).isTrue();
    }

    @Test
    void watchDirectory_registersRecursively() throws Exception {
        Path nested = Files.createDirectories(tempDir.resolve("a/b/c"));
        var t = createTracker();
        t.watchDirectory(tempDir);
        t.recordBaseline();

        Files.writeString(nested.resolve("test.java"), "content");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var changes = t.getAndClearChanges();
            assertThat(changes.added()).anyMatch(p -> p.toString().contains("test.java"));
        });
    }

    @Test
    void watchFile_onlyTracksSpecificFile() throws Exception {
        Path watched = Files.createFile(tempDir.resolve("watched.txt"));
        var t = createTracker();
        t.watchFile(watched);
        t.recordBaseline();

        // Create a sibling file that should NOT be tracked
        Files.writeString(tempDir.resolve("sibling.txt"), "ignore me");
        // Modify the watched file
        Files.writeString(watched, "modified");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var changes = t.getAndClearChanges();
            assertThat(changes.added().stream().anyMatch(p -> p.toString().contains("sibling.txt"))).isFalse();
            assertThat(changes.modified().stream().anyMatch(p -> p.toString().contains("sibling.txt"))).isFalse();
        });
    }

    @Test
    void recordBaseline_clearsAccumulatedEvents() throws Exception {
        var t = createTracker();
        t.watchDirectory(tempDir);

        // Create a file before baseline
        Files.writeString(tempDir.resolve("before.txt"), "content");
        Thread.sleep(500);

        t.recordBaseline();

        // Initial read should be empty (events cleared by baseline)
        var changes = t.getAndClearChanges();
        assertThat(changes.added().stream().anyMatch(p -> p.toString().contains("before.txt"))).isFalse();
    }

    @Test
    void recordBaseline_startsWatchThread() throws Exception {
        var t = createTracker();
        t.watchDirectory(tempDir);
        t.recordBaseline();

        assertThat(logger.hasMessage("info", "File watcher thread started")).isTrue();
    }

    @Test
    void recordBaseline_calledTwice_noSecondThread() throws Exception {
        var t = createTracker();
        t.watchDirectory(tempDir);
        t.recordBaseline();
        t.recordBaseline();

        long count = logger.messages("info").stream()
            .filter(e -> e.message().contains("File watcher thread started"))
            .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void getAndClearChanges_fileCreated_inAdded() throws Exception {
        var t = createTracker();
        t.watchDirectory(tempDir);
        t.recordBaseline();

        Files.writeString(tempDir.resolve("new.java"), "content");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var changes = t.getAndClearChanges();
            assertThat(changes.added()).anyMatch(p -> p.toString().contains("new.java"));
        });
    }

    @Test
    void getAndClearChanges_fileModified_inModified() throws Exception {
        Path file = Files.writeString(tempDir.resolve("existing.java"), "original");
        var t = createTracker();
        t.watchDirectory(tempDir);
        t.recordBaseline();

        Files.writeString(file, "modified-content");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var changes = t.getAndClearChanges();
            assertThat(changes.modified()).anyMatch(p -> p.toString().contains("existing.java"));
        });
    }

    @Test
    void getAndClearChanges_fileDeleted_inDeleted() throws Exception {
        Path file = Files.writeString(tempDir.resolve("toDelete.java"), "content");
        var t = createTracker();
        t.watchDirectory(tempDir);
        t.recordBaseline();

        Files.delete(file);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var changes = t.getAndClearChanges();
            assertThat(changes.deleted()).anyMatch(p -> p.toString().contains("toDelete.java"));
        });
    }

    @Test
    void getAndClearChanges_clearsAfterRead() throws Exception {
        var t = createTracker();
        t.watchDirectory(tempDir);
        t.recordBaseline();

        Files.writeString(tempDir.resolve("clearTest.java"), "content");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var changes = t.getAndClearChanges();
            assertThat(changes.added()).isNotEmpty();
        });

        // Second read should be empty
        var changes2 = t.getAndClearChanges();
        assertThat(changes2.isEmpty()).isTrue();
    }

    @Test
    void categorize_createThenModify_onlyInAdded() throws Exception {
        var t = createTracker();
        t.watchDirectory(tempDir);
        t.recordBaseline();

        Path file = tempDir.resolve("createModify.java");
        Files.writeString(file, "initial");
        Thread.sleep(200);
        Files.writeString(file, "modified");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var changes = t.getAndClearChanges();
            boolean inAdded = changes.added().stream().anyMatch(p -> p.toString().contains("createModify.java"));
            boolean inModified = changes.modified().stream().anyMatch(p -> p.toString().contains("createModify.java"));
            assertThat(inAdded).isTrue();
            assertThat(inModified).isFalse();
        });
    }

    @Test
    void categorize_createThenDelete_empty() throws Exception {
        var t = createTracker();
        t.watchDirectory(tempDir);
        t.recordBaseline();

        Path file = tempDir.resolve("createDelete.java");
        Files.writeString(file, "content");
        Thread.sleep(500);
        Files.delete(file);

        // Wait for events to be processed
        Thread.sleep(2000);
        var changes = t.getAndClearChanges();
        // File was created then deleted - should cancel out
        boolean inAdded = changes.added().stream().anyMatch(p -> p.toString().contains("createDelete.java"));
        boolean inDeleted = changes.deleted().stream().anyMatch(p -> p.toString().contains("createDelete.java"));
        assertThat(inAdded).isFalse();
        assertThat(inDeleted).isFalse();
    }

    @Test
    void categorize_modifyThenDelete_inDeleted() throws Exception {
        Path file = Files.writeString(tempDir.resolve("modDel.java"), "original");
        var t = createTracker();
        t.watchDirectory(tempDir);
        t.recordBaseline();

        Files.writeString(file, "modified");
        Thread.sleep(500);
        Files.delete(file);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var changes = t.getAndClearChanges();
            assertThat(changes.deleted()).anyMatch(p -> p.toString().contains("modDel.java"));
            assertThat(changes.modified()).noneMatch(p -> p.toString().contains("modDel.java"));
        });
    }

    @Test
    void categorize_deleteThenCreate_inAdded() throws Exception {
        Path file = Files.writeString(tempDir.resolve("delCreate.java"), "original");
        var t = createTracker();
        t.watchDirectory(tempDir);
        t.recordBaseline();

        Files.delete(file);
        Thread.sleep(500);
        Files.writeString(file, "recreated");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var changes = t.getAndClearChanges();
            assertThat(changes.added()).anyMatch(p -> p.toString().contains("delCreate.java"));
            assertThat(changes.deleted()).noneMatch(p -> p.toString().contains("delCreate.java"));
        });
    }

    @Test
    void close_shutsDownWatchServiceAndThread() throws Exception {
        var t = createTracker();
        t.watchDirectory(tempDir);
        t.recordBaseline();

        t.close();
        tracker = null; // prevent double-close in tearDown

        assertThat(logger.hasMessage("info", "Closing CompilationSourcesTracker")).isTrue();
    }

    @Test
    void close_isIdempotent() throws Exception {
        var t = createTracker();
        t.close();
        t.close(); // should not throw
        tracker = null;
    }

    @Test
    void newSubdirectory_autoRegistered() throws Exception {
        var t = createTracker();
        t.watchDirectory(tempDir);
        t.recordBaseline();

        Path newDir = tempDir.resolve("newsubdir");
        Files.createDirectory(newDir);
        Thread.sleep(500);
        Files.writeString(newDir.resolve("file.java"), "content");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var changes = t.getAndClearChanges();
            assertThat(changes.added()).anyMatch(p -> p.toString().contains("file.java"));
        });
    }

    @Test
    void logsChangeSummary() throws Exception {
        var t = createTracker();
        t.watchDirectory(tempDir);
        t.recordBaseline();

        Files.writeString(tempDir.resolve("summary.java"), "content");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var changes = t.getAndClearChanges();
            if (!changes.isEmpty()) {
                assertThat(logger.hasMessage("info", "Detected source changes")).isTrue();
            }
        });
    }

    @Test
    void logsNoChanges() throws Exception {
        var t = createTracker();
        t.watchDirectory(tempDir);
        t.recordBaseline();

        Thread.sleep(500);
        t.getAndClearChanges();
        assertThat(logger.hasMessage("debug", "No source changes detected")).isTrue();
    }
}
