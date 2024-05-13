package net.neoforged.gradle.dsl.common.util

import org.gradle.api.internal.tasks.DefaultSourceSet
import org.gradle.api.tasks.SourceSet

/**
 * Internal utilities for Gradle used in the DSL
 */
class GradleInternalUtils {

    private GradleInternalUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: GradleInternalUtils. This is a utility class")
    }

    /**
     * Gets the name of the source set with the given post fix
     *
     * @param sourceSet The source set to get the name of
     * @param postFix The post fix to append to the source set name
     * @return The name of the source set with the post fix
     */
    static String getSourceSetName(SourceSet sourceSet, String postFix) {
        if (sourceSet instanceof DefaultSourceSet) {
            return ((DefaultSourceSet) sourceSet).configurationNameOf(postFix);
        }

        final String capitalized = postFix.capitalize()
        final String name = sourceSet.getName() == SourceSet.MAIN_SOURCE_SET_NAME ? "" : sourceSet.getName().capitalize()

        return (name + capitalized).uncapitalize()
    }
}
