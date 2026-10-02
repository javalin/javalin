package io.javalin.dev.classloader;

import java.net.URL;
import java.net.URLClassLoader;

public class BaseClassLoader extends URLClassLoader {
    public BaseClassLoader(URL[] urls) {
        super(urls, ClassLoader.getPlatformClassLoader());
    }
}
