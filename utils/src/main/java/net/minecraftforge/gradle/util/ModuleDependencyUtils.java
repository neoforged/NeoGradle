package net.minecraftforge.gradle.util;

import org.gradle.api.artifacts.ModuleDependency;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for {@link ModuleDependency}s
 */
public final class ModuleDependencyUtils {

    private ModuleDependencyUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: ModuleDependencyUtils. This is a utility class");
    }

    /**
     * Gets the classifier of the first artifact of the given {@link ModuleDependency}
     *
     * @param moduleDependency The {@link ModuleDependency} to get the classifier from
     * @return The classifier of the first artifact of the given {@link ModuleDependency}, might be null
     */
    @Nullable
    public static String getClassifier(final ModuleDependency moduleDependency) {
        if (moduleDependency.getArtifacts().isEmpty())
            return null;

        return moduleDependency.getArtifacts().iterator().next().getClassifier();
    }

    /**
     * Gets the extension of the first artifact of the given {@link ModuleDependency}
     *
     * @param moduleDependency The {@link ModuleDependency} to get the extension from
     * @return The extension of the first artifact of the given {@link ModuleDependency}, might be null
     */
    @Nullable
    public static String getExtension(final ModuleDependency moduleDependency) {
        if (moduleDependency.getArtifacts().isEmpty())
            return null;

        return moduleDependency.getArtifacts().iterator().next().getExtension();
    }
}
