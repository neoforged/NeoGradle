package net.minecraftforge.gradle.util;

import org.gradle.api.artifacts.ResolvedDependency;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for {@link ResolvedDependency}s
 */
public final class ResolvedDependencyUtils {

    private ResolvedDependencyUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: ResolvedDependencyUtils. This is a utility class");
    }

    /**
     * Gets the classifier of the first artifact of the given {@link ResolvedDependency}
     *
     * @param resolvedDependency The {@link ResolvedDependency} to get the classifier from
     * @return The classifier of the first artifact of the given {@link ResolvedDependency}, might be null
     */
    @Nullable
    public static String getClassifier(final ResolvedDependency resolvedDependency) {
        if (resolvedDependency.getModuleArtifacts().isEmpty())
            return null;

        return resolvedDependency.getModuleArtifacts().iterator().next().getClassifier();
    }

    /**
     * Gets the extension of the first artifact of the given {@link ResolvedDependency}
     *
     * @param resolvedDependency The {@link ResolvedDependency} to get the extension from
     * @return The extension of the first artifact of the given {@link ResolvedDependency}, might be null
     */
    @Nullable
    public static String getExtension(final ResolvedDependency resolvedDependency) {
        if (resolvedDependency.getModuleArtifacts().isEmpty())
            return null;

        return resolvedDependency.getModuleArtifacts().iterator().next().getExtension();
    }
}
