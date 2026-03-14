package io.javalin.dev.maven;

import io.javalin.dev.compilation.CompilationInvoker;
import io.javalin.dev.compilation.CompilationResult;
import io.javalin.dev.log.JavalinDevLogger;
import org.apache.maven.cli.MavenCli;

import java.io.File;

public final class MavenCompilationInvoker implements CompilationInvoker {
    private final File baseDir;
    private final MavenCli maven;
    private final JavalinDevLogger logger;

    public MavenCompilationInvoker(File baseDir, JavalinDevLogger logger) {
        this.baseDir = baseDir;
        this.maven = new MavenCli();
        this.logger = logger;
        logger.debug("MavenCompilationInvoker created for base directory: " + baseDir.getAbsolutePath());
    }

    @Override
    public CompilationResult invoke() {
        try {
            logger.debug("Setting maven.multiModuleProjectDirectory to: " + baseDir.getAbsolutePath());
            System.setProperty("maven.multiModuleProjectDirectory", baseDir.getAbsolutePath());

            logger.info("Running: mvn compile -B in " + baseDir.getAbsolutePath());
            int exitCode = maven.doMain(
                new String[]{"compile", "-B"},
                baseDir.getAbsolutePath(),
                System.out,
                System.err
            );

            if (exitCode != 0) {
                logger.error("Maven compilation failed with exit code: " + exitCode);
                return CompilationResult.ERROR;
            }

            logger.info("Maven compilation succeeded");
            return CompilationResult.SUCCESS;
        } catch (Exception e) {
            logger.error("Maven compilation threw exception: " + e.getMessage());
            return CompilationResult.ERROR;
        }
    }
}
