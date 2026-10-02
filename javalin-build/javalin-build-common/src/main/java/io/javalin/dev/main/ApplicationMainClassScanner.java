package io.javalin.dev.main;

import io.javalin.dev.log.JavalinDevLogger;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public final class ApplicationMainClassScanner {
    private final JavalinDevLogger logger;

    public ApplicationMainClassScanner(JavalinDevLogger logger) {
        this.logger = logger;
    }

    public Map<ApplicationMainClassType, List<ApplicationMainClassCandidate>> scanDirectory(Path classesDir) throws IOException {
        if (!Files.isDirectory(classesDir)) {
            logger.warn("Classes directory does not exist: " + classesDir);
            return Map.of();
        }

        logger.info("Scanning for main class candidates in: " + classesDir);

        Map<ApplicationMainClassType, List<ApplicationMainClassCandidate>> candidates = new HashMap<>();
        Files.walkFileTree(classesDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Optional<ApplicationMainClassCandidate> candidate = scanFile(file);
                if (candidate.isEmpty()) {
                    return FileVisitResult.CONTINUE;
                }

                candidates.compute(candidate.get().type(), (ignored, candidates) -> {
                    if (candidates == null) {
                        candidates = new ArrayList<>();
                    }
                    candidates.add(candidate.get());
                    return candidates;
                });

                return FileVisitResult.CONTINUE;
            }
        });

        int totalCandidates = candidates.values().stream().mapToInt(List::size).sum();
        logger.info("Main class scan complete: found " + totalCandidates + " candidate(s) across " + candidates.size() + " type(s)");
        for (var entry : candidates.entrySet()) {
            for (var candidate : entry.getValue()) {
                logger.debug("  Candidate: " + candidate.className() + " [" + candidate.type() + "]");
            }
        }
        return Collections.unmodifiableMap(candidates);
    }

    public Optional<ApplicationMainClassCandidate> scanFile(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }

        // Using a PathMatcher is not worth the cost
        if (!file.toString().endsWith(".class")) {
            return Optional.empty();
        }

        try (InputStream in = Files.newInputStream(file)) {
            ClassReader cr = new ClassReader(in);
            MainMethodAsmVisitor detector = new MainMethodAsmVisitor();
            cr.accept(detector, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            return detector.result().map(type -> {
                String className = cr.getClassName().replace('/', '.');
                logger.debug("Found main method in: " + className + " [" + type + "]");
                return new ApplicationMainClassCandidate(className, type);
            });
        }
    }
}
