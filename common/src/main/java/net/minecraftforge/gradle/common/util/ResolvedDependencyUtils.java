package net.minecraftforge.gradle.common.util;

import org.gradle.api.artifacts.ResolvedDependency;
import org.jetbrains.annotations.Nullable;

public final class ResolvedDependencyUtils {

    private ResolvedDependencyUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: ResolvedDependencyUtils. This is a utility class");
    }

    @Nullable
    public static String getClassifier(final ResolvedDependency resolvedDependency) {
        return resolvedDependency.getModuleArtifacts().iterator().next().getClassifier();
    }

    @Nullable
    public static String getExtension(final ResolvedDependency resolvedDependency) {
        return resolvedDependency.getModuleArtifacts().iterator().next().getExtension();
    }
}
