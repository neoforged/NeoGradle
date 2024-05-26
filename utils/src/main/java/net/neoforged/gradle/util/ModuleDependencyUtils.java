package net.neoforged.gradle.util;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

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
     * Gets the classifier of the first artifact of the given {@link ModuleDependency}
     *
     * @param moduleDependency The {@link ModuleDependency} to get the classifier from
     * @return The classifier of the first artifact of the given {@link ModuleDependency}, might be empty if none is found.
     */
    @NotNull
    public static String getClassifierOrEmpty(final Dependency moduleDependency) {
        if (!(moduleDependency instanceof ModuleDependency))
            return "";

        return getClassifierOrEmpty((ModuleDependency) moduleDependency);
    }

    /**
     * Gets the classifier of the first artifact of the given {@link ModuleDependency}
     *
     * @param moduleDependency The {@link ModuleDependency} to get the classifier from
     * @return The classifier of the first artifact of the given {@link ModuleDependency}, might be empty if none is found.
     */
    @NotNull
    public static String getClassifierOrEmpty(final ModuleDependency moduleDependency) {
        final String artifactClassifier = getClassifier(moduleDependency);
        return artifactClassifier == null ? "" : artifactClassifier;
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

    /**
     * Gets the extension of the first artifact of the given {@link ModuleDependency}
     *
     * @param moduleDependency The {@link ModuleDependency} to get the extension from
     * @return The extension of the first artifact of the given {@link ModuleDependency}, might be empty if not found
     */
    @NotNull
    public static String getExtensionOrJar(final Dependency moduleDependency) {
        if (!(moduleDependency instanceof ModuleDependency))
            return "jar";

        return getExtensionOrJar((ModuleDependency) moduleDependency);
    }

    /**
     * Gets the extension of the first artifact of the given {@link ModuleDependency}
     *
     * @param moduleDependency The {@link ModuleDependency} to get the extension from
     * @return The extension of the first artifact of the given {@link ModuleDependency}, might be empty if not found
     */
    @NotNull
    public static String getExtensionOrJar(final ModuleDependency moduleDependency) {
        final String artifactExtension = getExtension(moduleDependency);
        return artifactExtension == null ? "jar" : artifactExtension;
    }

    /**
     * Formats the given {@link Dependency} into a string
     *
     * @param dependency The {@link Dependency} to format
     * @return The formatted string
     */
    public static String format(final Dependency dependency) {
        if (!(dependency instanceof ModuleDependency))
            throw new IllegalArgumentException("Dependency is not a ModuleDependency");

        return format((ModuleDependency) dependency);
    }

    /**
     * Formats the given {@link Dependency} into a string
     *
     * @param dependency The {@link Dependency} to format
     * @return The formatted string
     */
    public static String format(final ModuleDependency dependency) {
        return format(dependency.getGroup(), dependency.getName(), dependency.getVersion(), getClassifierOrEmpty(dependency), getExtensionOrJar(dependency));
    }

    /**
     * Formats the given group, name, version, classifier and extension into a string
     *
     * @param group      The group of the dependency
     * @param name       The name of the dependency
     * @param version    The version of the dependency
     * @param classifier The classifier of the dependency
     * @param extension  The extension of the dependency
     * @return The formatted string
     */
    public static String format(@Nullable String group, String name, String version, String classifier, String extension) {
       final StringBuilder builder = new StringBuilder();


        if (group != null && !group.trim().isEmpty()) {
            builder.append(group);
            builder.append(":");
        }

        builder.append(name);

        builder.append(":");
        builder.append(version);

        if (classifier != null && !classifier.trim().isEmpty()) {
            builder.append(":");
            builder.append(classifier);
        }

        if (extension != null && !extension.trim().isEmpty() && !extension.trim().toLowerCase(Locale.ROOT).equals("jar")) {
            builder.append("@")
                    .append(extension);
        }

        return builder.toString();
    }

    public static String toConfigurationName(String s) {
        return s.toLowerCase(Locale.ROOT).replace('-', '_').replace('.', '_').replace(":","_");
    }

    public static String toConfigurationName(Dependency dependency) {
        return toConfigurationName(format(dependency));
    }
}
