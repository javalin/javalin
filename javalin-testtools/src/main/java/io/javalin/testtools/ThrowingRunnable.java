package io.javalin.testtools;

@FunctionalInterface
public interface ThrowingRunnable extends Runnable {
    @Override
    default void run() {
        try {
            tryRun();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    void tryRun() throws Exception;
}
