package io.javalin.dev.classloader;

import java.net.URL;
import java.net.URLClassLoader;

public class RuntimeClassLoader extends URLClassLoader {
    public RuntimeClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // Already loaded?
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }

            // JDK classes must be delegated
            if (isJdkClassName(name)) {
                return super.loadClass(name, resolve);
            }

            // Child-first: try own URLs before parent
            try {
                c = findClass(name);
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            } catch (ClassNotFoundException e) {
                // Fall through to parent
            }

            return super.loadClass(name, resolve);
        }
    }

    private static boolean isJdkClassName(String name) {
        return name.startsWith("java.")
               || name.startsWith("javax.")
               || name.startsWith("jdk.")
               || name.startsWith("sun.")
               || name.startsWith("com.sun.");
    }
}
