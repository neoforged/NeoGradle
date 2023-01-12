package net.minecraftforge.gradle.common.util;

import org.gradle.api.internal.plugins.ExtensionContainerInternal;
import org.gradle.api.plugins.ExtensionContainer;

import org.gradle.api.logging.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

public final class GradleInternalUtils {

    private GradleInternalUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: GradleInternalUtils. This is a utility class");
    }

    public static Collection<Object> getExtensions(final ExtensionContainer container) {
        return ((ExtensionContainerInternal) container).getAsMap().values();
    }

    public static ProgressLoggerWrapper getProgressLogger(final Logger logger, final Object serviceOwner, final String name) {
        try {
            final ProgressLoggerWrapper wrapper = new ProgressLoggerWrapper(logger);
            wrapper.init(serviceOwner, name);
            return wrapper;
        } catch (final ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to get progress logger", e);
        }
    }

    /**
     * A wrapper for a progress logger.
     * The usage of this logger varies between gradle versions a tiny bit, so we use reflection to handle it.
     * <p>
     * Carbon copy of: <<a href="https://github.com/michel-kraemer/gradle-download-task/blob/94ec0dad3b6831acc00db1807741d7feee57a5fc/src/main/java/de/undercouch/gradle/tasks/download/internal/ProgressLoggerWrapper.java#L1">...</a>
     */
    public static class ProgressLoggerWrapper {
        private final Logger logger;
        private Object progressLogger;

        private String size;
        private String destFileName;
        private long processedBytes = 0;
        private long loggedKb = 0;

        /**
         * Create a progress logger wrapper
         * @param logger the Gradle logger
         */
        private ProgressLoggerWrapper(Logger logger) {
            this.logger = logger;
        }

        /**
         * Initialize the progress logger wrapper
         * @param servicesOwner the Gradle services owner
         * @param src the URL to the file to be downloaded
         * @throws ClassNotFoundException if one of Gradle's internal classes
         * could not be found
         * @throws NoSuchMethodException if the interface of one of Gradle's
         * internal classes has changed
         * @throws InvocationTargetException if a method from one of Gradle's
         * internal classes could not be invoked
         * @throws IllegalAccessException if a method from one of Gradle's
         * internal classes is not accessible
         */
        private void init(Object servicesOwner, String src) throws ClassNotFoundException,
                NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            // we are about to access an internal class. Use reflection here to provide
            // as much compatibility to different Gradle versions as possible

            // get ProgressLoggerFactory class
            Class<?> progressLoggerFactoryClass = Class.forName(
                    "org.gradle.internal.logging.progress.ProgressLoggerFactory");

            //get ProgressLoggerFactory service
            Object serviceFactory = invoke(servicesOwner, "getServices");
            Object progressLoggerFactory = invoke(serviceFactory, "get",
                    progressLoggerFactoryClass);

            //get actual progress logger
            progressLogger = invoke(progressLoggerFactory, "newOperation", getClass());

            //configure progress logger
            String desc = "Download " + src;
            invoke(progressLogger, "setDescription", desc);
            logger.lifecycle(desc);
        }

        /**
         * Invoke a method using reflection
         * @param obj the object whose method should be invoked
         * @param method the name of the method to invoke
         * @param args the arguments to pass to the method
         * @return the method's return value
         * @throws NoSuchMethodException if the method was not found
         * @throws InvocationTargetException if the method could not be invoked
         * @throws IllegalAccessException if the method could not be accessed
         */
        private static Object invoke(Object obj, String method, Object... args)
                throws NoSuchMethodException, InvocationTargetException,
                IllegalAccessException {
            Class<?>[] argumentTypes = new Class[args.length];
            for (int i = 0; i < args.length; ++i) {
                argumentTypes[i] = args[i].getClass();
            }
            Method m = findMethod(obj, method, argumentTypes);
            m.setAccessible(true);
            return m.invoke(obj, args);
        }

        /**
         * Uses reflection to find a method with the given name and argument types
         * from the given object or its superclasses.
         * @param obj the object
         * @param methodName the name of the method to return
         * @param argumentTypes the method's argument types
         * @return the method object
         * @throws NoSuchMethodException if the method could not be found
         */
        private static Method findMethod(Object obj, String methodName,
                                         Class<?>[] argumentTypes) throws NoSuchMethodException {
            Class<?> clazz = obj.getClass();
            while (clazz != null) {
                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.getName().equals(methodName) &&
                            Arrays.equals(method.getParameterTypes(), argumentTypes)) {
                        return method;
                    }
                }
                clazz = clazz.getSuperclass();
            }
            throw new NoSuchMethodException("Method " + methodName + "(" +
                    Arrays.toString(argumentTypes) + ") on " + obj.getClass());
        }

        /**
         * Invoke a method using reflection but don't throw any exceptions.
         * Just log errors instead.
         * @param obj the object whose method should be invoked
         * @param method the name of the method to invoke
         * @param args the arguments to pass to the method
         */
        private void invokeIgnoreExceptions(Object obj, String method,
                                            Object... args) {
            try {
                invoke(obj, method, args);
            } catch (ReflectiveOperationException e) {
                logger.trace("Unable to log progress", e);
            }
        }

        /**
         * Start on operation
         */
        public void started() {
            if (progressLogger != null) {
                invokeIgnoreExceptions(progressLogger, "started");
            }
        }

        /**
         * Complete an operation
         */
        public void completed() {
            if (progressLogger != null) {
                invokeIgnoreExceptions(progressLogger, "completed");
            }
        }

        /**
         * Set the current operation's progress
         * @param msg the progress message
         */
        private void progress(String msg) {
            if (progressLogger != null) {
                invokeIgnoreExceptions(progressLogger, "progress", msg);
            }
        }

        /**
         * The total number of bytes to process and reset progress
         * @param size the total size
         */
        public void setSize(long size) {
            this.size = toLengthText(size);
            processedBytes = 0;
            loggedKb = 0;
        }

        /**
         * Set the name of the destination file
         * @param destFileName the file name
         */
        public void setDestFileName(String destFileName) {
            this.destFileName = destFileName;
        }

        /**
         * Increment the number of bytes processed
         * @param increment the increment
         */
        public void incrementDownloadProgress(long increment) {
            processedBytes += increment;

            if (progressLogger == null) {
                return;
            }

            long processedKb = processedBytes / 1024;
            if (processedKb > loggedKb) {
                StringBuilder sb = new StringBuilder();
                if (destFileName != null) {
                    sb.append(destFileName);
                    sb.append(" > ");
                }
                sb.append(toLengthText(processedBytes));
                if (size != null) {
                    sb.append("/");
                    sb.append(size);
                }
                sb.append(" downloaded");
                progress(sb.toString());
                loggedKb = processedKb;
            }
        }

        /**
         * Converts a number of bytes to a human-readable string
         * @param bytes the bytes
         * @return the human-readable string
         */
        private static String toLengthText(long bytes) {
            if (bytes < 1024) {
                return bytes + " B";
            } else if (bytes < 1024 * 1024) {
                return (bytes / 1024) + " KB";
            } else if (bytes < 1024 * 1024 * 1024) {
                return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
            } else {
                return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
            }
        }
    }
}
