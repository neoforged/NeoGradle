package net.minecraftforge.gradle.common.util;

import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.internal.logging.slf4j.ContextAwareTaskLogger;
import org.jetbrains.annotations.Nullable;

/**
 * This class contains utilities which touch gradle internals, this might break in the future.
 */
public final class GradleInternalUtils {

    private GradleInternalUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: GradleInternalUtils. This is a utility class");
    }

    public static void adaptJavaCompileLoggerToHideNotes(final JavaCompile compile) {
        final ContextAwareTaskLogger logger = (ContextAwareTaskLogger) compile.getLogger();
        logger.setMessageRewriter((logLevel, s) -> {
            if (s.startsWith("Note:"))
                return "";
            return null;
        });
    }
}
