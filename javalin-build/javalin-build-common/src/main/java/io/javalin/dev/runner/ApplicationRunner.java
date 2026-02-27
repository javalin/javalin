package io.javalin.dev.runner;

import io.javalin.dev.classloader.BaseClassLoader;
import io.javalin.dev.classloader.RuntimeClassLoader;
import io.javalin.dev.log.JavalinDevLogger;
import io.javalin.dev.main.ApplicationMainClassCandidate;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Arrays;
import java.util.OptionalInt;
import java.util.concurrent.CountDownLatch;

public class ApplicationRunner {
    private final URL[] dependencyUrls;
    private final URL[] classesUrls;
    private final ApplicationMainClassCandidate mainClass;
    private final JavalinDevLogger logger;
    private final Object startLock;
    private volatile BaseClassLoader baseClassLoader;

    public ApplicationRunner(URL[] dependencyUrls, URL[] classesUrls, ApplicationMainClassCandidate mainClass, JavalinDevLogger logger) {
        this.dependencyUrls = dependencyUrls;
        this.classesUrls = classesUrls;
        this.mainClass = mainClass;
        this.logger = logger;
        this.startLock = new Object();
        logger.debug("ApplicationRunner created for main class: " + mainClass.className() + " [" + mainClass.type() + "]");
        logger.debug("Dependency URLs: " + Arrays.toString(dependencyUrls));
        logger.debug("Classes URLs: " + Arrays.toString(classesUrls));
    }

    public ApplicationInstance start() throws Exception {
        synchronized (startLock) {
            logger.info("Starting new application instance...");

            var internalPort = findFreePort();
            if (internalPort.isEmpty()) {
                logger.error("Could not find any available port");
                throw new IllegalStateException("Could not find any available port");
            }
            logger.info("Allocated internal port: " + internalPort.getAsInt());
            System.setProperty("javalin.dev.internalPort", String.valueOf(internalPort.getAsInt()));

            if (baseClassLoader == null) {
                logger.debug("Creating BaseClassLoader with " + dependencyUrls.length + " dependency URL(s)");
                baseClassLoader = new BaseClassLoader(dependencyUrls);
            }

            logger.debug("Creating RuntimeClassLoader with " + classesUrls.length + " classes URL(s)");
            RuntimeClassLoader runtimeCl = new RuntimeClassLoader(classesUrls, baseClassLoader);

            logger.debug("Loading main class: " + mainClass.className());
            Class<?> mainClass = runtimeCl.loadClass(this.mainClass.className());
            logger.debug("Main class loaded successfully: " + mainClass.getName());

            CountDownLatch started = new CountDownLatch(1);
            Thread appThread = new Thread(() -> {
                Thread.currentThread().setContextClassLoader(runtimeCl);
                started.countDown();
                try {
                    logger.debug("Invoking main method on thread: " + Thread.currentThread().getName());
                    invokeMain(mainClass);
                } catch (Exception e) {
                    logger.error("Application main method threw an exception: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            appThread.setDaemon(true);
            appThread.start();
            logger.debug("Application thread started: " + appThread.getName() + ", waiting for latch...");

            started.await();
            logger.info("Application instance started on port " + internalPort.getAsInt());

            return new ApplicationInstance(internalPort.getAsInt(), runtimeCl, appThread, logger);
        }
    }

    private static OptionalInt findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            var result = socket.getLocalPort();
            return OptionalInt.of(result);
        } catch (IOException e) {
            return OptionalInt.empty();
        }
    }

    private void invokeMain(Class<?> mainClazz) throws Exception {
        logger.debug("Invoking main method via reflection for type: " + mainClass.type());
        switch (mainClass.type()) {
            case STATIC_MAIN_WITH_ARGS -> {
                Method m = mainClazz.getDeclaredMethod("main", String[].class);
                m.setAccessible(true);
                m.invoke(null, (Object) new String[0]);
            }
            case STATIC_MAIN_WITHOUT_ARGS -> {
                Method m = mainClazz.getDeclaredMethod("main");
                m.setAccessible(true);
                m.invoke(null);
            }
            case INSTANCE_MAIN_WITH_ARGS -> {
                Method m = mainClazz.getDeclaredMethod("main", String[].class);
                m.setAccessible(true);
                logger.debug("Creating instance of " + mainClazz.getName() + " via zero-arg constructor");
                Object obj = mainClazz.getDeclaredConstructor().newInstance();
                m.invoke(obj, (Object) new String[0]);
            }
            case INSTANCE_MAIN_WITHOUT_ARGS -> {
                Method m = mainClazz.getDeclaredMethod("main");
                logger.debug("Creating instance of " + mainClazz.getName() + " via zero-arg constructor");
                Object obj = mainClazz.getDeclaredConstructor().newInstance();
                m.invoke(obj);
            }
        }
    }
}
