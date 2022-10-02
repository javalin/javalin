package io.javalin.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectiveVirtualThreadBuilder {

    private Object /*Thread.Builder.OfVirtual*/ builder = Thread.class.getMethod("ofVirtual").invoke(Thread.class);

    public ReflectiveVirtualThreadBuilder() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    }

    public ReflectiveVirtualThreadBuilder name(String name) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method nameMethod = builder.getClass().getMethod("name", String.class);
        builder = nameMethod.invoke(builder, name);
        return this;
    }

    public Thread unstarted(Runnable runnable) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method unstartedMethod = builder.getClass().getMethod("unstarted", Runnable.class);
        return (Thread) unstartedMethod.invoke(builder, runnable);
    }

}
