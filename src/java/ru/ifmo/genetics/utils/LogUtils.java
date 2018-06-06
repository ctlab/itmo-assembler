package ru.ifmo.genetics.utils;

import org.apache.log4j.Logger;

public class LogUtils {
    private static Logger uncaughtLogger = Logger.getLogger("uncaught");
    private static Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler =
            new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    uncaughtLogger.warn("Uncaught exception in thread " + t, e);
                }
            };

    public static void registerUncaughtExceptionLogger() {
        Thread.setDefaultUncaughtExceptionHandler(defaultUncaughtExceptionHandler);
    }
}
