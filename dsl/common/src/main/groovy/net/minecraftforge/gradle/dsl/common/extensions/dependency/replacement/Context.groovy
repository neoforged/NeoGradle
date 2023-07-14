package net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement;

import groovy.transform.CompileStatic;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable;

/**
 * Defines the context of a dependency replacement.
 */
@CompileStatic
interface Context {

    /**
     * The project inside of which a dependency replacement is being performed.
     *
     * @return The project inside of which a dependency replacement is being performed.
     */
    @NotNull
    Project getProject();

    /**
     * The configuration in which a dependency replacement is being performed.
     *
     * @return The configuration in which a dependency replacement is being performed.
     */
    @NotNull
    Configuration getConfiguration();

    /**
     * The dependency which is being replaced.
     *
     * @return The dependency which is being replaced.
     */
    @NotNull
    ModuleDependency getDependency();

    /**
     * The parent dependency replacement context, if any.
     *
     * @return The parent dependency replacement context, if any.
     */
    @Nullable
    Context getParent();

}
