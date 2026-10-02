package io.javalin.dev.maven;

import io.javalin.dev.compilation.CompilationInvoker;
import io.javalin.dev.compilation.CompilationResult;
import io.javalin.dev.compilation.CompilationSourcesTracker;
import io.javalin.dev.log.JavalinDevLogger;
import io.javalin.dev.main.ApplicationMainClassCandidate;
import io.javalin.dev.main.ApplicationMainClassScanner;
import io.javalin.dev.main.ApplicationMainClassType;
import io.javalin.dev.proxy.ApplicationProxy;
import io.javalin.dev.runner.ApplicationInstance;
import io.javalin.dev.runner.ApplicationRunner;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mojo(name = "dev", requiresDependencyResolution = ResolutionScope.RUNTIME, requiresProject = true)
public final class JavalinDevMojo extends AbstractMojo {
    /**
     * The current Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * All projects in the reactor (populated automatically by Maven).
     * Respects {@code -pl} filtering: only modules selected by the user are included.
     */
    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    /**
     * The fully qualified main class name.
     * If not specified, it is auto-detected by scanning for main method candidates.
     */
    @Parameter(property = "main.class")
    private String mainClassName;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        JavalinDevLogger logger = new MavenDevLogger(getLog());

        logger.info("Starting dev mode...");

        try {
            CompilationInvoker compiler = new MavenCompilationInvoker(project.getBasedir(), logger);

            compileApplication(compiler, logger);

            ApplicationRunner runner = createApplicationRunner(logger);

            ApplicationInstance instance = runner.start();

            try (CompilationSourcesTracker tracker = new CompilationSourcesTracker(logger)) {
                registerWatchedSources(tracker, logger);
                tracker.recordBaseline();

                var proxyPort = waitForAppInit(logger);
                ApplicationProxy proxy = createApplicationProxy(tracker, compiler, runner, instance, logger);
                proxy.start(proxyPort);
            }
        } catch (MojoExecutionException | MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Dev mode failed", e);
        }
    }

    private void compileApplication(CompilationInvoker compiler, JavalinDevLogger logger) throws MojoExecutionException {
        logger.info("Compiling...");
        CompilationResult initialCompile = compiler.invoke();
        if (initialCompile == CompilationResult.ERROR) {
            throw new MojoExecutionException("Initial compilation failed");
        }
        logger.info("Compilation successful");
    }


    private @NotNull ApplicationRunner createApplicationRunner(JavalinDevLogger logger) throws MojoExecutionException {
        URL[] dependencyUrls = resolveDependencyUrls(logger);
        URL[] classesUrls = resolveClassesUrls(logger);
        ApplicationMainClassCandidate resolvedMainClass = resolveMainClass(logger);
        return new ApplicationRunner(dependencyUrls, classesUrls, resolvedMainClass, logger);
    }

    private URL[] resolveDependencyUrls(JavalinDevLogger logger) throws MojoExecutionException {
        try {
            List<URL> urls = new ArrayList<>();
            for (String element : project.getRuntimeClasspathElements()) {
                // Skip the output directory (those go in RuntimeClassLoader)
                if (element.equals(project.getBuild().getOutputDirectory())) {
                    continue;
                }
                urls.add(new File(element).toURI().toURL());
            }
            logger.debug("Resolved " + urls.size() + " dependency URL(s)");
            return urls.toArray(URL[]::new);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve classpath", e);
        }
    }

    private URL[] resolveClassesUrls(JavalinDevLogger logger) throws MojoExecutionException {
        try {
            File outputDir = new File(project.getBuild().getOutputDirectory());
            logger.debug("Classes output directory: " + outputDir.getAbsolutePath());
            return new URL[]{outputDir.toURI().toURL()};
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve classes directory", e);
        }
    }

    private ApplicationMainClassCandidate resolveMainClass(JavalinDevLogger logger) throws MojoExecutionException {
        try {
            Path classesDir = Path.of(project.getBuild().getOutputDirectory());
            ApplicationMainClassScanner scanner = new ApplicationMainClassScanner(logger);
            if (mainClassName != null && !mainClassName.isBlank()) {
                logger.info("Using configured main class: " + mainClassName);
                Path mainClass = classesDir.resolve(mainClassName.replaceAll("\\.", File.separator) + ".class");
                var result = scanner.scanFile(mainClass);
                if (result.isEmpty()) {
                    throw new MojoExecutionException("Cannot find main class: " + mainClassName);
                }
                return result.get();
            } else {
                logger.info("No main class configured, auto-detecting...");
                Map<ApplicationMainClassType, List<ApplicationMainClassCandidate>> candidates = scanner.scanDirectory(classesDir);
                for (var mainClassType : ApplicationMainClassType.values()) {
                    var candidatesByType = candidates.get(mainClassType);
                    if (candidatesByType == null || candidatesByType.isEmpty()) {
                        continue;
                    }

                    if (candidatesByType.size() == 1) {
                        logger.info("Auto-detected main class: " + candidatesByType.get(0).className() + " [" + candidatesByType.get(0).type() + "]");
                        return candidatesByType.get(0);
                    }

                    var candidatesByTypeClassNames = candidatesByType.stream()
                        .map(ApplicationMainClassCandidate::className)
                        .collect(Collectors.joining(", "));
                    throw new MojoExecutionException(
                        "Multiple main classes found: " + candidatesByTypeClassNames
                        + ". Specify with -Dmain.class=com.example.App");
                }
                throw new MojoExecutionException(
                    "No main classes found"
                    + ". Create one or specify with -Dmain.class=com.example.App");
            }
        }
        catch (IOException e) {
            throw new MojoExecutionException("Failed to scan for main class", e);
        }
    }

    private void registerWatchedSources(CompilationSourcesTracker tracker, JavalinDevLogger logger) throws IOException {
        logger.info("Registering watched sources...");
        // Watch the current project and its parent chain
        var current = project;
        while (current != null) {
            watchProjectSources(current, tracker, logger);
            current = current.getParent();
        }

        // Watch reactor dependencies (sibling modules in the same build)
        Map<String, MavenProject> reactorsByIdentifier = reactorProjects.stream().collect(Collectors.toMap(
            p -> p.getGroupId() + ":" + p.getArtifactId(),
            Function.identity(),
            (ignored, last) -> last
        ));

       if (project != null) {
           for (Artifact artifact : project.getArtifacts()) {
               String artifactIdentifier = artifact.getGroupId() + ":" + artifact.getArtifactId();
               MavenProject reactorDep = reactorsByIdentifier.get(artifactIdentifier);
               if (reactorDep != null) {
                   logger.info("Watching reactor dependency: " + reactorDep.getGroupId() + ":" + reactorDep.getArtifactId());
                   watchProjectSources(reactorDep, tracker, logger);
               }

               // Watch system-scoped dependencies (those with a systemPath)
               if (Artifact.SCOPE_SYSTEM.equals(artifact.getScope()) && artifact.getFile() != null) {
                   logger.info("Watching system dependency: " + artifact.getFile().getAbsolutePath());
                   tracker.watchFile(artifact.getFile().toPath());
               }
           }
       }
       logger.info("Watched sources registration complete");
    }

    private void watchProjectSources(MavenProject p, CompilationSourcesTracker tracker, JavalinDevLogger logger) throws IOException {
        if (p == null) {
            return;
        }

        List<MavenProject> collectedProjects = p.getCollectedProjects();
        if (collectedProjects != null) {
            for (MavenProject collected : collectedProjects) {
                watchProjectSources(collected, tracker, logger);
            }
        }

        logger.debug("Watching project: " + p.getGroupId() + ":" + p.getArtifactId());

        // Project POM
        File pom = p.getFile();
        if (pom != null) {
            tracker.watchFile(pom.toPath());
        }

        // Source directories
        for (String sourceRoot : p.getCompileSourceRoots()) {
            tracker.watchDirectory(Path.of(sourceRoot));
        }

        // Resource directories
        for (Resource resource : p.getResources()) {
            if (resource != null) {
                String directory = resource.getDirectory();
                if (directory != null) {
                    File dir = new File(directory);
                    if (!dir.isAbsolute() && p.getBasedir() != null) {
                        dir = new File(p.getBasedir(), directory);
                    }
                    tracker.watchDirectory(dir.toPath());
                }
            }
        }
    }

    private @NotNull ApplicationProxy createApplicationProxy(CompilationSourcesTracker tracker, CompilationInvoker compiler, ApplicationRunner runner, ApplicationInstance instance, JavalinDevLogger logger) {
        return new ApplicationProxy(tracker, compiler, runner, instance, logger);
    }

    private int waitForAppInit(JavalinDevLogger logger) throws MojoExecutionException {
        logger.debug("Waiting for application to set javalin.dev.requestedPort...");
        while (!Thread.interrupted()) {
            String value = System.getProperty("javalin.dev.requestedPort");
            if (value != null) {
                int port = Integer.parseInt(value);
                logger.info("Application requested proxy port: " + port);
                return port;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new MojoExecutionException("Could not detect requested port, defaulting to 8080");
    }
}
