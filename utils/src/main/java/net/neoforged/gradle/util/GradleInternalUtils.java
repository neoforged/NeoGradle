package net.neoforged.gradle.util;

import groovy.lang.Closure;
import org.gradle.api.Task;
import org.gradle.api.internal.GeneratedSubclass;
import org.gradle.api.internal.plugins.ExtensionContainerInternal;
import org.gradle.api.internal.tasks.TaskExecutionOutcome;
import org.gradle.api.internal.tasks.TaskStateInternal;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.util.internal.ConfigureUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Utility class for internal gradle classes.
 */
public final class GradleInternalUtils {

    private GradleInternalUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: GradleInternalUtils. This is a utility class");
    }

    /**
     * Gets the extensions from the given container.
     *
     * @param container the container
     * @return the extensions
     */
    public static Collection<Object> getExtensions(final ExtensionContainer container) {
        return ((ExtensionContainerInternal) container).getAsMap().values();
    }

    /**
     * Configures the given delegate using the given closure.
     * Considering the given delegate as the this-variable in the closure.
     *
     * @param closure The closure to configure the delegate with.
     * @param t The delegate to configure.
     * @return The configured delegate.
     * @param <T> The type of the delegate.
     */
    public static <T> T configureSelf(Closure<?> closure, T t) {
        return ConfigureUtil.configureSelf(closure, t);
    }

    /**
     * Configures the given delegate using the given properties map.
     *
     * @param properties The properties map to configure the delegate with
     * @param delegate The delegate to configure
     * @return The configured delegate
     * @param <T> The type of the delegate
     */
    public static <T> T configureByMap(Map<?, ?> properties, T delegate) {
        return ConfigureUtil.configureByMap(properties, delegate);
    }

    /**
     * Creates a new progress logger that wraps the given logger and can be used to log progression of an action.
     *
     * @param logger The logger to write to
     * @param serviceOwner The service owner
     * @param name The name of the action
     * @return The progress logger
     */
    public static ProgressLoggerWrapper getProgressLogger(final Logger logger, final Object serviceOwner, final String name) {
        try {
            final ProgressLoggerWrapper wrapper = new ProgressLoggerWrapper(logger);
            wrapper.init(serviceOwner, name);
            return wrapper;
        } catch (final ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to get progress logger", e);
        }
    }

    public static void setTaskUpToDate(Task targetTask, final String reason) {
        ((TaskStateInternal) targetTask.getState()).setOutcome(TaskExecutionOutcome.UP_TO_DATE);
        ((TaskStateInternal) targetTask.getState()).setSkipReasonMessage(reason);
        ((TaskStateInternal) targetTask.getState()).setDidWork(false);

    }

    public static void setTaskFromCache(Task targetTask, String reason) {
        ((TaskStateInternal) targetTask.getState()).setOutcome(TaskExecutionOutcome.FROM_CACHE);
        ((TaskStateInternal) targetTask.getState()).setSkipReasonMessage(reason);
        ((TaskStateInternal) targetTask.getState()).setDidWork(false);
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
        private String actionType = "downloaded";

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
                NoSuchMethodException, NoSuchFieldException, InvocationTargetException, IllegalAccessException {
            // we are about to access an internal class. Use reflection here to provide
            // as much compatibility to different Gradle versions as possible

            // get ProgressLoggerFactory class
            Class<?> progressLoggerFactoryClass = Class.forName(
                    "org.gradle.internal.logging.progress.ProgressLoggerFactory");

            //get ProgressLoggerFactory service
            Object serviceFactory;
            try {
                serviceFactory = invoke(servicesOwner, "getServices");
            } catch (Throwable ignored) {
                // Is there no `getServices` method? Then try a `services` field
                serviceFactory = getFieldValue(servicesOwner, "services");
            }

            Object progressLoggerFactory = invoke(serviceFactory, "get",
                    progressLoggerFactoryClass);

            //get actual progress logger
            progressLogger = invoke(progressLoggerFactory, "newOperation", getClass());

            //configure progress logger
            String desc = "Download " + src;
            invoke(progressLogger, "setDescription", desc);
        }

        /**
         * Looks up the reflection target class from a given class.
         * Currently takes care of the decoration of Gradle's internal classes.
         *
         * @param cls The class to look up the target class for
         * @return The target class
         */
        private static Class<?> getReflectionTarget(Class<?> cls) {
            if (Arrays.asList(cls.getInterfaces()).contains(GeneratedSubclass.class)) {
                return cls.getSuperclass();
            }

            return cls;
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
         * Get the value of a field using reflection.
         * @param obj the object whose field should be got
         * @param name the name of the field to get
         * @return the field's value
         */
        @SuppressWarnings("SameParameterValue")
        private static Object getFieldValue(Object obj, String name)
                throws NoSuchFieldException, IllegalAccessException {
            Class<?> clazz = getReflectionTarget(obj.getClass());
            while (clazz != null) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    if (field.getName().equals(name)) {
                        field.setAccessible(true);
                        return field.get(obj);
                    }
                }
                clazz = clazz.getSuperclass();
            }

            throw new NoSuchFieldException("Field " + name + " on " + obj.getClass());
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
            Class<?> clazz = getReflectionTarget(obj.getClass());
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
         * Sets the action type of the logger.
         * By default, this is 'downloaded' but can be changed.
         *
         * @param actionType The action type to set.
         */
        public void setActionType(String actionType) {
            this.actionType = actionType;
        }

        /**
         * Increment the number of bytes processed
         * @param increment the increment
         */
        public void incrementDownloadProgress(long increment) {
            processedBytes += increment;
            setActionType("downloaded");

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
                sb.append(" ").append(actionType);
                progress(sb.toString());
                loggedKb = processedKb;
            }
        }

        /**
         * Increments the processed file count by one.
         * The logger can only be either used in download mode or in file mode, but not combined.
         */
        public void incrementProcessedFileCount() {
            processedBytes++;

            if (progressLogger == null) {
                return;
            }

             if (processedBytes > loggedKb) {
                 loggedKb = processedBytes;
                    StringBuilder sb = new StringBuilder();
                    if (destFileName != null) {
                        sb.append(destFileName);
                        sb.append(" > ");
                    }
                    sb.append(toCountText(processedBytes));
                    if (size != null) {
                        sb.append("/");
                        sb.append(size);
                    }
                    sb.append(" ").append(actionType);
                    progress(sb.toString());
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

        /**
         * Converts a number of bytes to a human-readable string
         * @param bytes the bytes
         * @return the human-readable string
         */
        private static String toCountText(long bytes) {
            if (bytes < 1024) {
                return String.valueOf(bytes);
            } else if (bytes < 1024 * 1024) {
                return (bytes / 1024) + "k";
            } else if (bytes < 1024 * 1024 * 1024) {
                return String.format("%.2fm", bytes / (1024.0 * 1024.0));
            } else {
                return String.format("%.2fg", bytes / (1024.0 * 1024.0 * 1024.0));
            }
        }
    }
}
